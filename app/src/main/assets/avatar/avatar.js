let scene, camera, renderer, currentVrm, clock;
let cameraPreset = 'fullBody';
let isSpeaking = false;
let speechInterval = null;

function initScene() {
    const container = document.getElementById('canvas-container');
    if (!container || typeof THREE === 'undefined') {
        showPlaceholder();
        if (window.AndroidBridge) AndroidBridge.modelError("Three.js not loaded");
        return;
    }

    scene = new THREE.Scene();
    clock = new THREE.Clock();

    camera = new THREE.PerspectiveCamera(30, container.clientWidth / container.clientHeight, 0.1, 20);
    updateCameraPosition();

    const dirLight = new THREE.DirectionalLight(0xffffff, 1.1);
    dirLight.position.set(1, 2, 1).normalize();
    scene.add(dirLight);
    scene.add(new THREE.AmbientLight(0xffffff, 0.55));

    renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.setSize(container.clientWidth, container.clientHeight);
    container.appendChild(renderer.domElement);

    window.addEventListener('resize', onWindowResize);
    animate();
}

function onWindowResize() {
    const container = document.getElementById('canvas-container');
    if (!camera || !renderer || !container) return;
    camera.aspect = container.clientWidth / container.clientHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(container.clientWidth, container.clientHeight);
}

function updateCameraPosition() {
    if (!camera) return;
    if (cameraPreset === 'closeUp') {
        camera.position.set(0, 1.45, 0.85);
    } else {
        camera.position.set(0, 1.15, 2.4);
    }
    camera.lookAt(0, 1.15, 0);
}

function showPlaceholder() {
    const canvas = document.getElementById('canvas-container');
    const placeholder = document.getElementById('placeholder-container');
    if (canvas) canvas.style.display = 'none';
    if (placeholder) placeholder.style.display = 'flex';
}

function showCanvas() {
    const canvas = document.getElementById('canvas-container');
    const placeholder = document.getElementById('placeholder-container');
    if (canvas) canvas.style.display = 'block';
    if (placeholder) placeholder.style.display = 'none';
}

function loadModel(vrmUrl) {
    if (typeof THREE === 'undefined') {
        showPlaceholder();
        if (window.AndroidBridge) AndroidBridge.modelError("Three.js missing");
        return;
    }

    if (!scene) initScene();

    // Remove previous model
    if (currentVrm) {
        scene.remove(currentVrm.scene);
        currentVrm = null;
    }

    const loader = new THREE.GLTFLoader();

    // Try to use three-vrm if available
    if (typeof THREE.VRM !== 'undefined' && THREE.VRM.from) {
        // Older three-vrm style
        loader.load(vrmUrl, (gltf) => {
            THREE.VRM.from(gltf).then((vrm) => {
                currentVrm = vrm;
                scene.add(vrm.scene);
                showCanvas();

                let triangles = 0;
                vrm.scene.traverse((obj) => {
                    if (obj.isMesh && obj.geometry) {
                        const geo = obj.geometry;
                        if (geo.index) triangles += geo.index.count / 3;
                        else if (geo.attributes.position) triangles += geo.attributes.position.count / 3;
                    }
                });

                if (window.AndroidBridge) {
                    AndroidBridge.modelLoaded(Math.round(triangles));
                }
            }).catch((err) => {
                console.error(err);
                showPlaceholder();
                if (window.AndroidBridge) AndroidBridge.modelError("VRM.from failed");
            });
        }, undefined, (error) => {
            console.error(error);
            showPlaceholder();
            if (window.AndroidBridge) AndroidBridge.modelError(error.message || "Load failed");
        });
    } else {
        // Fallback - just load as normal GLTF
        loader.load(vrmUrl, (gltf) => {
            currentVrm = { scene: gltf.scene };
            scene.add(gltf.scene);
            showCanvas();
            if (window.AndroidBridge) AndroidBridge.modelLoaded(0);
        }, undefined, (error) => {
            console.error(error);
            showPlaceholder();
            if (window.AndroidBridge) AndroidBridge.modelError(error.message || "Load failed");
        });
    }
}

function setExpression(name) {
    if (!currentVrm) return;

    // For older three-vrm
    if (currentVrm.blendShapeProxy) {
        currentVrm.blendShapeProxy.setValue('neutral', 0);
        currentVrm.blendShapeProxy.setValue('happy', 0);
        currentVrm.blendShapeProxy.setValue('angry', 0);
        currentVrm.blendShapeProxy.setValue('sad', 0);
        currentVrm.blendShapeProxy.setValue('relaxed', 0);

        const map = {
            neutral: 'neutral',
            happy: 'happy',
            teasing: 'happy',
            jealous: 'angry',
            sleepy: 'relaxed',
            caring: 'happy'
        };
        const target = map[name] || 'neutral';
        try {
            currentVrm.blendShapeProxy.setValue(target, 1.0);
        } catch (e) {}
    }
}

function playGesture(name) {
    console.log("Gesture:", name);
}

function setCamera(preset) {
    cameraPreset = preset;
    updateCameraPosition();
}

function startSimulatedSpeech() {
    isSpeaking = true;
    if (speechInterval) clearInterval(speechInterval);

    speechInterval = setInterval(() => {
        if (!currentVrm) return;
        const open = 0.3 + Math.random() * 0.5;

        if (currentVrm.blendShapeProxy) {
            try {
                currentVrm.blendShapeProxy.setValue('a', open);
            } catch (e) {}
        }
    }, 90);
}

function stopSpeech() {
    isSpeaking = false;
    if (speechInterval) {
        clearInterval(speechInterval);
        speechInterval = null;
    }
    if (currentVrm && currentVrm.blendShapeProxy) {
        try {
            currentVrm.blendShapeProxy.setValue('a', 0);
        } catch (e) {}
    }
}

function speakAudio(base64Audio) {
    startSimulatedSpeech();
}

function animate() {
    requestAnimationFrame(animate);
    const delta = clock ? clock.getDelta() : 0.016;

    if (currentVrm && currentVrm.update) {
        currentVrm.update(delta);
    }

    if (renderer && scene && camera) {
        renderer.render(scene, camera);
    }
}

// Make functions available to Android
window.loadModel = loadModel;
window.setExpression = setExpression;
window.playGesture = playGesture;
window.setCamera = setCamera;
window.startSimulatedSpeech = startSimulatedSpeech;
window.stopSpeech = stopSpeech;
window.speakAudio = speakAudio;

window.onerror = function (msg, url, line) {
    if (window.AndroidBridge) {
        AndroidBridge.modelError("JS Error: " + msg + " (line " + line + ")");
    }
};