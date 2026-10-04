// Mika 3D avatar viewer (modern module version: three.js 0.180 + three-vrm 3).

import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { VRMLoaderPlugin, VRMUtils } from '@pixiv/three-vrm';

// If her arms point UP instead of down, change ARM_FLIP to -1.
// If her nod looks backwards, change NOD_FLIP to -1.
const ARM_FLIP = 1;
const NOD_FLIP = 1;
const ARM_DOWN = 1.3;

let scene, camera, renderer, clock, holder, currentVrm = null;
let t = 0, frameAcc = 0, broken = false;
let cameraPreset = 'fullBody';
let modelHeight = 1.6;
let cam = { y: 0.8, z: 3.5, look: 0.75, ty: 0.8, tz: 3.5, tlook: 0.75, fy: 0.8, fz: 3.5, flook: 0.75, cy: 1.3, cz: 1.4, clook: 1.3 };
let yaw = 0, yawTarget = 0, lastDrag = 0, dragging = false, lastX = 0;
const armSign = { l: -ARM_FLIP, r: ARM_FLIP };
const nodSign = NOD_FLIP;
const bones = {};
const BONE_NAMES = ['hips', 'spine', 'chest', 'neck', 'head', 'leftShoulder', 'rightShoulder',
    'leftUpperArm', 'rightUpperArm', 'leftLowerArm', 'rightLowerArm'];

const EMOTIONS = {
    neutral: {},
    happy: { happy: 1 },
    teasing: { relaxed: 0.7, happy: 0.3 },
    jealous: { angry: 0.35, sad: 0.3 },
    sleepy: { relaxed: 0.4, blink: 0.45 },
    caring: { happy: 0.35, sad: 0.25 }
};
const EXPR_KEYS = ['happy', 'relaxed', 'angry', 'sad', 'blink'];
const VOWELS = ['aa', 'ih', 'ou', 'ee', 'oh'];
const cur = { happy: 0, relaxed: 0, angry: 0, sad: 0, blink: 0 };
let target = {};
const mouth = { aa: 0, ih: 0, ou: 0, ee: 0, oh: 0 };
let speaking = false, nextVowel = 0, vowel = 'aa', vowelAmp = 0.5;
let nextBlink = 2, blinkStart = -1;
let gesture = null;
let audioCtx = null, analyser = null, audioSrc = null;

const GESTURE_SECONDS = { wave: 2.4, nod: 1.4, headTilt: 1.6, shrug: 1.4, armsCrossed: 3.0, handsOnHips: 3.0 };

function bridge(fn, arg) {
    try {
        if (window.AndroidBridge && window.AndroidBridge[fn]) window.AndroidBridge[fn](arg);
    } catch (e) { /* ignore */ }
}

function showPlaceholder() {
    document.getElementById('canvas-container').style.display = 'none';
    document.getElementById('placeholder-container').style.display = 'flex';
}

function showCanvas() {
    document.getElementById('canvas-container').style.display = 'block';
    document.getElementById('placeholder-container').style.display = 'none';
}

function fail(e) {
    const msg = (e && e.message) ? e.message : String(e);
    console.warn('Avatar problem: ' + msg);
    showPlaceholder();
    bridge('modelError', msg);
}

function resize() {
    const container = document.getElementById('canvas-container');
    if (!container || !renderer || !camera) return;
    const w = container.clientWidth || window.innerWidth;
    const h = container.clientHeight || window.innerHeight;
    renderer.setSize(w, h);
    camera.aspect = w / h;
    camera.updateProjectionMatrix();
}

function initScene() {
    const container = document.getElementById('canvas-container');
    // The container must be visible BEFORE we measure it.
    showCanvas();
    const w = container.clientWidth || window.innerWidth;
    const h = container.clientHeight || window.innerHeight;

    scene = new THREE.Scene();
    clock = new THREE.Clock();
    camera = new THREE.PerspectiveCamera(30, w / h, 0.1, 100);
    camera.position.set(0, 1, 3);

    const dirLight = new THREE.DirectionalLight(0xffffff, Math.PI);
    dirLight.position.set(1, 2, 2);
    scene.add(dirLight);
    scene.add(new THREE.AmbientLight(0xffffff, 0.8));
    scene.add(camera);

    renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    renderer.setClearColor(0x120E18, 1);
    renderer.setSize(w, h);
    container.appendChild(renderer.domElement);

    setupPointer();
    window.addEventListener('resize', resize);
    if (window.ResizeObserver) new ResizeObserver(resize).observe(container);
    animate();
}

function setupPointer() {
    const el = renderer.domElement;
    el.style.touchAction = 'none';
    el.addEventListener('pointerdown', function (e) {
        dragging = true; lastX = e.clientX; lastDrag = performance.now();
        try { el.setPointerCapture(e.pointerId); } catch (err) { /* ignore */ }
    });
    el.addEventListener('pointermove', function (e) {
        if (!dragging) return;
        yawTarget += (e.clientX - lastX) * 0.01;
        lastX = e.clientX; lastDrag = performance.now();
    });
    const end = function () { dragging = false; lastDrag = performance.now(); };
    el.addEventListener('pointerup', end);
    el.addEventListener('pointercancel', end);
}

function clearModel() {
    if (holder) {
        scene.remove(holder);
        try { VRMUtils.deepDispose(holder); } catch (e) { /* ignore */ }
    }
    holder = null; currentVrm = null; gesture = null;
    for (const k in bones) delete bones[k];
}

function loadModel(url) {
    try {
        if (!renderer) { initScene(); } else { showCanvas(); resize(); }
    } catch (e) { fail(e); return; }

    clearModel();
    const loader = new GLTFLoader();
    loader.register(function (parser) { return new VRMLoaderPlugin(parser); });

    loader.load(url,
        function (gltf) {
            const vrm = gltf.userData.vrm;
            if (!vrm) { fail(new Error('This file is not a valid VRM model.')); return; }
            try {
                if (VRMUtils.removeUnnecessaryVertices) VRMUtils.removeUnnecessaryVertices(gltf.scene);
                if (VRMUtils.combineSkeletons) VRMUtils.combineSkeletons(gltf.scene);
            } catch (e) { console.warn('Optimize skipped: ' + e); }
            finishLoad(vrm);
        },
        function (ev) {
            if (ev && ev.lengthComputable) bridge('loadProgress', Math.round(ev.loaded / ev.total * 100));
        },
        function (err) {
            fail(new Error('Could not read the model file: ' + (err && err.message ? err.message : err)));
        }
    );
}

function finishLoad(vrm) {
    try {
        currentVrm = vrm;
        VRMUtils.rotateVRM0(vrm); // turns old VRM 0.x models to face the camera
        vrm.scene.traverse(function (o) { o.frustumCulled = false; });

        holder = new THREE.Group();
        holder.add(vrm.scene);
        scene.add(holder);

        const box = new THREE.Box3().setFromObject(holder);
        holder.position.y = -box.min.y; // feet on the floor
        modelHeight = Math.max(0.5, box.max.y - box.min.y);

        BONE_NAMES.forEach(function (name) {
            try {
                const node = vrm.humanoid.getNormalizedBoneNode(name);
                if (node) bones[name] = { node: node, rest: node.quaternion.clone() };
            } catch (e) { /* bone missing, skip */ }
        });

        if (vrm.lookAt) { try { vrm.lookAt.target = camera; } catch (e) { /* ignore */ } }

        setupCameraPresets();
        target = {};

        let tris = 0;
        vrm.scene.traverse(function (o) {
            if (o.isMesh && o.geometry) {
                const g = o.geometry;
                tris += g.index ? g.index.count / 3 : (g.attributes.position ? g.attributes.position.count / 3 : 0);
            }
        });
        console.log('Avatar ready, triangles: ' + Math.round(tris));
        bridge('modelLoaded', Math.round(tris));
    } catch (e) { fail(e); }
}

function setupCameraPresets() {
    const tan = Math.tan(15 * Math.PI / 180);
    cam.flook = modelHeight * 0.45;
    cam.fy = modelHeight * 0.5;
    cam.fz = (modelHeight * 1.2 / 2) / tan;
    cam.clook = modelHeight * 0.86;
    cam.cy = modelHeight * 0.87;
    cam.cz = modelHeight * 0.85;
    applyPreset(cameraPreset, true);
}

function applyPreset(name, snap) {
    const close = name === 'closeUp';
    cam.ty = close ? cam.cy : cam.fy;
    cam.tz = close ? cam.cz : cam.fz;
    cam.tlook = close ? cam.clook : cam.flook;
    if (snap) { cam.y = cam.ty; cam.z = cam.tz; cam.look = cam.tlook; }
}

function setBlend(key, weight) {
    if (!currentVrm || !currentVrm.expressionManager) return;
    try { currentVrm.expressionManager.setValue(key, weight); } catch (e) { /* model lacks it */ }
}

function smooth(x) { return x * x * (3 - 2 * x); }
function envelope(p) {
    return smooth(Math.min(1, p / 0.2)) * smooth(Math.min(1, (1 - p) / 0.2));
}

function applyGesture(name, p, add) {
    const e = envelope(p);
    const sL = armSign.l, sR = armSign.r;
    if (name === 'wave') {
        add('rightUpperArm', 0, 0, -sR * 2.0 * e);
        add('rightLowerArm', 0, 0, Math.sin(p * Math.PI * 8) * 0.45 * e);
    } else if (name === 'nod') {
        add('head', nodSign * Math.sin(p * Math.PI * 4) * 0.22 * e, 0, 0);
    } else if (name === 'headTilt') {
        add('head', 0, 0, 0.28 * e);
        add('neck', 0, 0, 0.1 * e);
    } else if (name === 'shrug') {
        add('leftShoulder', 0, 0, -sL * 0.28 * e);
        add('rightShoulder', 0, 0, -sR * 0.28 * e);
        add('head', 0, 0, 0.08 * e);
    } else if (name === 'armsCrossed') { // approximate
        add('leftUpperArm', 0, -0.5 * e, 0);
        add('rightUpperArm', 0, 0.5 * e, 0);
        add('leftLowerArm', 0, -2.0 * e, 0);
        add('rightLowerArm', 0, 2.0 * e, 0);
    } else if (name === 'handsOnHips') { // approximate
        add('leftUpperArm', 0, 0, -sL * 0.45 * e);
        add('rightUpperArm', 0, 0, -sR * 0.45 * e);
        add('leftLowerArm', 0, -2.2 * e, 0);
        add('rightLowerArm', 0, 2.2 * e, 0);
    }
}

function analyserLevel() {
    if (!analyser) return 0;
    const data = new Uint8Array(analyser.fftSize);
    analyser.getByteTimeDomainData(data);
    let sum = 0;
    for (let i = 0; i < data.length; i++) { const v = (data[i] - 128) / 128; sum += v * v; }
    return Math.min(1, Math.sqrt(sum / data.length) * 4);
}

function updateAvatar(step) {
    const now = performance.now();

    // rotate by dragging, then slowly return to the front
    if (!dragging && now - lastDrag > 3000) yawTarget *= (1 - Math.min(1, step * 1.5));
    yaw += (yawTarget - yaw) * Math.min(1, step * 8);
    holder.rotation.y = yaw;

    // smooth camera move
    const k = Math.min(1, step * 4);
    cam.y += (cam.ty - cam.y) * k;
    cam.z += (cam.tz - cam.z) * k;
    cam.look += (cam.tlook - cam.look) * k;
    camera.position.set(0, cam.y, cam.z);
    camera.lookAt(0, cam.look, 0);

    // body pose
    const pose = {};
    const add = function (name, x, y, z) {
        const a = pose[name] || (pose[name] = [0, 0, 0]);
        a[0] += x; a[1] += y; a[2] += z;
    };
    add('spine', Math.sin(t * 1.6) * 0.015, 0, 0);
    add('chest', Math.sin(t * 1.6 + 0.4) * 0.015, 0, 0);
    add('head', Math.sin(t * 0.7) * 0.02, Math.sin(t * 0.45) * 0.06, 0);
    add('hips', 0, 0, Math.sin(t * 0.5) * 0.02);
    add('leftUpperArm', 0, 0, armSign.l * ARM_DOWN);
    add('rightUpperArm', 0, 0, armSign.r * ARM_DOWN);
    if (gesture) {
        const p = (now - gesture.start) / (gesture.dur * 1000);
        if (p >= 1) gesture = null; else applyGesture(gesture.name, p, add);
    }
    const euler = new THREE.Euler(0, 0, 0, 'ZYX');
    const q = new THREE.Quaternion();
    for (const name in bones) {
        const a = pose[name] || [0, 0, 0];
        euler.set(a[0], a[1], a[2], 'ZYX');
        q.setFromEuler(euler);
        bones[name].node.quaternion.copy(bones[name].rest).multiply(q);
    }

    // face expressions
    const ek = Math.min(1, step * 6);
    EXPR_KEYS.forEach(function (key) { cur[key] += ((target[key] || 0) - cur[key]) * ek; });
    if (t >= nextBlink) { blinkStart = t; nextBlink = t + 2 + Math.random() * 4; }
    const blinkAuto = (blinkStart >= 0 && t - blinkStart < 0.18) ? Math.sin((t - blinkStart) / 0.18 * Math.PI) : 0;
    setBlend('happy', cur.happy);
    setBlend('relaxed', cur.relaxed);
    setBlend('angry', cur.angry);
    setBlend('sad', cur.sad);
    setBlend('blink', Math.max(blinkAuto, cur.blink));

    // mouth (lip sync)
    const mouthTarget = { aa: 0, ih: 0, ou: 0, ee: 0, oh: 0 };
    if (speaking) {
        if (analyser) {
            mouthTarget.aa = analyserLevel();
        } else {
            if (t >= nextVowel) {
                nextVowel = t + 0.11;
                vowel = VOWELS[Math.floor(Math.random() * VOWELS.length)];
                vowelAmp = 0.3 + Math.random() * 0.5;
            }
            mouthTarget[vowel] = vowelAmp;
        }
    }
    const mk = Math.min(1, step * 18);
    VOWELS.forEach(function (v) {
        mouth[v] += (mouthTarget[v] - mouth[v]) * mk;
        setBlend(v, mouth[v]);
    });

    currentVrm.update(step);
}

function animate() {
    requestAnimationFrame(animate);
    if (broken || document.hidden || !renderer) return;
    try {
        const dt = Math.min(clock.getDelta(), 0.1);
        frameAcc += dt;
        if (frameAcc < 1 / 30) return; // about 30 fps to save battery
        const step = frameAcc;
        frameAcc = 0;
        t += step;
        if (currentVrm && holder) updateAvatar(step);
        renderer.render(scene, camera);
    } catch (e) {
        broken = true;
        fail(e);
    }
}

// ---- Functions the Android app calls ----
function setExpression(name) {
    target = EMOTIONS[String(name || '').toLowerCase()] || {};
}

function playGesture(name) {
    if (GESTURE_SECONDS[name]) gesture = { name: name, start: performance.now(), dur: GESTURE_SECONDS[name] };
}

function setCamera(preset) {
    cameraPreset = preset === 'closeUp' ? 'closeUp' : 'fullBody';
    if (currentVrm) applyPreset(cameraPreset, false);
}

function startSimulatedSpeech() { speaking = true; }

function stopSpeech() {
    speaking = false;
    analyser = null;
    if (audioSrc) { try { audioSrc.stop(); } catch (e) { /* ignore */ } audioSrc = null; }
}

async function speakAudio(base64) {
    try {
        stopSpeech();
        const bin = atob(base64);
        const bytes = new Uint8Array(bin.length);
        for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
        const AC = window.AudioContext || window.webkitAudioContext;
        if (!audioCtx) audioCtx = new AC();
        if (audioCtx.state === 'suspended') await audioCtx.resume();
        const buf = await audioCtx.decodeAudioData(bytes.buffer);
        const src = audioCtx.createBufferSource();
        src.buffer = buf;
        const an = audioCtx.createAnalyser();
        an.fftSize = 256;
        src.connect(an);
        an.connect(audioCtx.destination);
        src.onended = function () { speaking = false; analyser = null; audioSrc = null; };
        audioSrc = src; analyser = an; speaking = true;
        src.start(0);
    } catch (e) {
        console.warn('Audio lip sync fallback: ' + e);
        startSimulatedSpeech();
    }
}

window.loadModel = loadModel;
window.setExpression = setExpression;
window.playGesture = playGesture;
window.setCamera = setCamera;
window.startSimulatedSpeech = startSimulatedSpeech;
window.stopSpeech = stopSpeech;
window.speakAudio = speakAudio;

window.onerror = function (msg, url, line) {
    console.warn('JS error: ' + msg + ' (line ' + line + ')');
};

// The engine has finished downloading: run any model request that arrived early.
window.__moduleReady = true;
if (window.__pending) {
    const early = window.__pending;
    window.__pending = null;
    loadModel(early);
}