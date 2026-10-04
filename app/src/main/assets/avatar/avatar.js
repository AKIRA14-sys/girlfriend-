import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { VRMLoaderPlugin, VRMUtils } from '@pixiv/three-vrm';

let scene, camera, renderer, currentVrm;
let cameraPreset = 'fullBody';
let currentExpression = 'neutral';
let activeGesture = null;
let gestureTimer = null;
let isSpeaking = false;
let speechSimInterval = null;
let audioContext, analyser, audioSource;

// User rotation control
let userRotationY = 0;
let rotationResetTimer = null;
let isDragging = false;
let previousTouchX = 0;

function initScene() {
    const container = document.getElementById('canvas-container');
    if (!container) return;

    scene = new THREE.Scene();
    camera = new THREE.PerspectiveCamera(30, container.clientWidth / container.clientHeight, 0.1, 20.0);
    updateCameraPosition();

    const dirLight = new THREE.DirectionalLight(0xffffff, 1.0);
    dirLight.position.set(1.0, 2.0, 1.0).normalize();
    scene.add(dirLight);

    const ambLight = new THREE.AmbientLight(0xffffff, 0.6);
    scene.add(ambLight);

    renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.setSize(container.clientWidth, container.clientHeight);
    container.appendChild(renderer.domElement);

    window.addEventListener('resize', onWindowResize);
    setupTouchControls(container);
    animate();
}

function updateCameraPosition() {
    if (!camera) return;
    if (cameraPreset === 'closeUp') {
        camera.position.set(0.0, 1.4, 0.8);
    } else {
        camera.position.set(0.0, 1.0, 2.6);
    }
    camera.lookAt(0.0, 1.0, 0.0);
}

function setupTouchControls(container) {
    container.addEventListener('pointerdown', (e) => {
        isDragging = true;
        previousTouchX = e.clientX;
        if (rotationResetTimer) clearTimeout(rotationResetTimer);
    });

    container.addEventListener('pointermove', (e) => {
        if (!isDragging || !currentVrm) return;
        const deltaX = e.clientX - previousTouchX;
        previousTouchX = e.clientX;
        userRotationY += deltaX * 0.01;
        currentVrm.scene.rotation.y = userRotationY;
    });

    const stopDrag = () => {
        if (!isDragging) return;
        isDragging = false;
        rotationResetTimer = setTimeout(() => {
            userRotationY = 0;
            if (currentVrm) currentVrm.scene.rotation.y = 0;
        }, 3000);
    };

    container.addEventListener('pointerup', stopDrag);
    container.addEventListener('pointercancel', stopDrag);
}

function loadModel(vrmUrl) {
    const loader = new GLTFLoader();
    loader.register((parser) => new VRMLoaderPlugin(parser));

    loader.load(
        vrmUrl,
        (gltf) => {
            const vrm = gltf.userData.vrm;
            if (currentVrm) {
                scene.remove(currentVrm.scene);
                VRMUtils.deepDispose(currentVrm.scene);
            }
            currentVrm = vrm;
            VRMUtils.removeUnnecessaryVertices(gltf.scene);
            VRMUtils.rotateVRM0(vrm);
            scene.add(vrm.scene);

            let totalTriangles = 0;
            vrm.scene.traverse((obj) => {
                if (obj.isMesh && obj.geometry) {
                    const geom = obj.geometry;
                    if (geom.index) totalTriangles += geom.index.count / 3;
                    else if (geom.attributes.position) totalTriangles += geom.attributes.position.count / 3;
                }
            });

            document.getElementById('canvas-container').style.display = 'block';
            document.getElementById('placeholder-container').style.display = 'none';

            if (!renderer) initScene();

            if (window.AndroidBridge) {
                AndroidBridge.modelLoaded(Math.round(totalTriangles));
            }
        },
        (progress) => {
            if (progress.lengthComputable && window.AndroidBridge) {
                const percent = Math.round((progress.loaded / progress.total) * 100);
                AndroidBridge.loadProgress(percent);
            }
        },
        (error) => {
            console.error("VRM Load Error:", error);
            showPlaceholder();
            if (window.AndroidBridge) {
                AndroidBridge.modelError(error.message || "Failed to parse 3D VRM model.");
            }
        }
    );
}

function showPlaceholder() {
    const canvasContainer = document.getElementById('canvas-container');
    const placeholderContainer = document.getElementById('placeholder-container');
    if (canvasContainer) canvasContainer.style.display = 'none';
    if (placeholderContainer) placeholderContainer.style.display = 'flex';
}

function setExpression(name) {
    currentExpression = name;
    if (!currentVrm || !currentVrm.expressionManager) return;

    const presets = ['neutral', 'happy', 'angry', 'sad', 'relaxed', 'surprised', 'aa', 'ih', 'ou', 'ee', 'oh', 'joy', 'fun', 'sorrow', 'A', 'I', 'U', 'E', 'O', 'Joy', 'Fun', 'Angry', 'Sorrow', 'Surprised', 'Neutral'];
    presets.forEach(p => {
        try { currentVrm.expressionManager.setValue(p, 0); } catch(e){}
    });

    switch(name) {
        case 'happy':
            setExp('happy', 1.0); setExp('joy', 1.0); setExp('Joy', 1.0); break;
        case 'teasing':
            setExp('relaxed', 0.6); setExp('fun', 0.6); setExp('Fun', 0.6); setExp('happy', 0.4); setExp('joy', 0.4); setExp('Joy', 0.4); break;
        case 'jealous':
            setExp('angry', 0.4); setExp('Angry', 0.4); setExp('sad', 0.4); setExp('sorrow', 0.4); setExp('Sorrow', 0.4); break;
        case 'sleepy':
            setExp('blink', 0.5); setExp('Blink', 0.5); setExp('relaxed', 0.4); setExp('fun', 0.4); break;
        case 'caring':
            setExp('happy', 0.5); setExp('joy', 0.5); setExp('sad', 0.3); setExp('sorrow', 0.3); break;
        default:
            setExp('neutral', 1.0); setExp('Neutral', 1.0); break;
    }
}

function setExp(name, val) {
    try {
        if (currentVrm && currentVrm.expressionManager) {
            currentVrm.expressionManager.setValue(name, val);
        }
    } catch(e) {}
}

function playGesture(name) {
    activeGesture = name;
    if (gestureTimer) clearTimeout(gestureTimer);
    gestureTimer = setTimeout(() => {
        activeGesture = null;
    }, 2500);
}

function setCamera(preset) {
    cameraPreset = preset;
    updateCameraPosition();
}

function startSimulatedSpeech() {
    isSpeaking = true;
    if (speechSimInterval) clearInterval(speechSimInterval);

    const mouthShapes = ['aa', 'ih', 'ou', 'ee', 'oh', 'A', 'I', 'U', 'E', 'O'];
    speechSimInterval = setInterval(() => {
        if (!isSpeaking) return;
        const randomMouth = mouthShapes[Math.floor(Math.random() * mouthShapes.length)];
        mouthShapes.forEach(m => setExp(m, 0));
        setExp(randomMouth, 0.4 + Math.random() * 0.5);
    }, 150);
}

function stopSpeech() {
    isSpeaking = false;
    if (speechSimInterval) clearInterval(speechSimInterval);
    ['aa', 'ih', 'ou', 'ee', 'oh', 'A', 'I', 'U', 'E', 'O'].forEach(m => setExp(m, 0));
}

function speakAudio(base64Audio) {
    try {
        const audioBytes = Uint8Array.from(atob(base64Audio), c => c.charCodeAt(0));
        const blob = new Blob([audioBytes], { type: 'audio/mp3' });
        const url = URL.createObjectURL(blob);
        const audio = new Audio(url);

        if (!audioContext) {
            audioContext = new (window.AudioContext || window.webkitAudioContext)();
        }
        analyser = audioContext.createAnalyser();
        analyser.fftSize = 256;

        audioSource = audioContext.createMediaElementSource(audio);
        audioSource.connect(analyser);
        analyser.connect(audioContext.destination);

        isSpeaking = true;
        audio.play();

        const dataArray = new Uint8Array(analyser.frequencyBinCount);
        const checkLoudness = () => {
            if (!isSpeaking) return;
            analyser.getByteFrequencyData(dataArray);
            let sum = 0;
            for (let i = 0; i < dataArray.length; i++) sum += dataArray[i];
            const avg = sum / dataArray.length;
            const volume = Math.min(avg / 100.0, 1.0);

            setExp('aa', volume);
            setExp('A', volume);

            if (!audio.ended) {
                requestAnimationFrame(checkLoudness);
            } else {
                stopSpeech();
            }
        };
        checkLoudness();

        audio.onended = () => { stopSpeech(); };
    } catch(e) {
        startSimulatedSpeech();
    }
}

const clock = new THREE.Clock();
let nextBlinkTime = 2.0;
let blinkTimer = 0;

function animate() {
    requestAnimationFrame(animate);
    const delta = clock.getDelta();

    if (currentVrm) {
        currentVrm.update(delta);

        const time = clock.getElapsedTime();
        if (currentVrm.humanoid) {
            const spine = currentVrm.humanoid.getRawBoneNode('spine');
            if (spine) {
                spine.rotation.z = Math.sin(time * 1.2) * 0.02;
                spine.rotation.x = Math.sin(time * 2.0) * 0.015;
            }
            const head = currentVrm.humanoid.getRawBoneNode('head');
            if (head) {
                if (activeGesture === 'nod') {
                    head.rotation.x = Math.sin(time * 10.0) * 0.15;
                } else if (activeGesture === 'headTilt') {
                    head.rotation.z = 0.2;
                } else {
                    head.rotation.y = Math.sin(time * 0.8) * 0.03;
                }
            }

            const rightArm = currentVrm.humanoid.getRawBoneNode('rightUpperArm');
            if (rightArm && activeGesture === 'wave') {
                rightArm.rotation.z = 1.2 + Math.sin(time * 8.0) * 0.2;
            }
            const leftArm = currentVrm.humanoid.getRawBoneNode('leftUpperArm');
            if (leftArm && rightArm && activeGesture === 'armsCrossed') {
                leftArm.rotation.z = -0.8;
                rightArm.rotation.z = 0.8;
            }
        }

        blinkTimer += delta;
        if (blinkTimer > nextBlinkTime) {
            setExp('blink', 1.0);
            setExp('Blink', 1.0);
            setTimeout(() => { setExp('blink', 0.0); setExp('Blink', 0.0); }, 150);
            blinkTimer = 0;
            nextBlinkTime = 2.0 + Math.random() * 4.0;
        }
    }

    if (renderer && scene && camera) {
        renderer.render(scene, camera);
    }
}

function onWindowResize() {
    const container = document.getElementById('canvas-container');
    if (!container || !renderer || !camera) return;
    camera.aspect = container.clientWidth / container.clientHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(container.clientWidth, container.clientHeight);
}

window.loadModel = loadModel;
window.setExpression = setExpression;
window.playGesture = playGesture;
window.setCamera = setCamera;
window.startSimulatedSpeech = startSimulatedSpeech;
window.stopSpeech = stopSpeech;
window.speakAudio = speakAudio;

window.onerror = function(msg, url, line) {
    if (window.AndroidBridge) {
        AndroidBridge.modelError("JS Error: " + msg + " (line " + line + ")");
    }
};

initScene();
