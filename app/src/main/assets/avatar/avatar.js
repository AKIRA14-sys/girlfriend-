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
    scene.background = new THREE.Color(0x120E18); // dark background

    clock = new THREE.Clock();

    camera = new THREE.PerspectiveCamera(30, container.clientWidth / container.clientHeight, 0.1, 100);
    camera.position.set(0, 1.4, 3.0);
    camera.lookAt(0, 1.2, 0);

    // Stronger lights
    const dirLight = new THREE.DirectionalLight(0xffffff, 1.4);
    dirLight.position.set(1, 2, 2);
    scene.add(dirLight);

    const ambLight = new THREE.AmbientLight(0xffffff, 0.7);
    scene.add(ambLight);

    renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.setSize(container.clientWidth, container.clientHeight);
    renderer.setClearColor(0x120E18, 1);
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

function showPlaceholder() {
    document.getElementById('canvas-container').style.display = 'none';
    document.getElementById('placeholder-container').style.display = 'flex';
}

function showCanvas() {
    document.getElementById('canvas-container').style.display = 'block';
    document.getElementById('placeholder-container').style.display = 'none';
}

function loadModel(vrmUrl) {
    console.log("Loading VRM:", vrmUrl);

    if (typeof THREE === 'undefined') {
        if (window.AndroidBridge) AndroidBridge.modelError("Three.js missing");
        return;
    }

    if (!scene) initScene();
    showCanvas();

    // Remove old model
    if (currentVrm) {
        scene.remove(currentVrm.scene);
        currentVrm = null;
    }

    const loader = new THREE.GLTFLoader();

    loader.load(
        vrmUrl,
        function (gltf) {
            console.log("GLTF loaded successfully");

            // Try different ways to get VRM
            let vrm = null;

            if (gltf.userData && gltf.userData.vrm) {
                vrm = gltf.userData.vrm;
            } else if (typeof THREE.VRM !== 'undefined' && THREE.VRM.from) {
                // Older three-vrm
                THREE.VRM.from(gltf).then(function (v) {
                    finishLoad(v);
                }).catch(function (err) {
                    console.error("VRM.from failed", err);
                    // Fallback: just show the raw gltf
                    finishLoad({ scene: gltf.scene });
                });
                return;
            } else {
                // Just use the raw scene
                vrm = { scene: gltf.scene };
            }

            finishLoad(vrm);
        },
        function (progress) {
            // loading progress
        },
        function (error) {
            console.error("Load error:", error);
            if (window.AndroidBridge) {
                AndroidBridge.modelError("Failed to load: " + (error.message || error));
            }
            showPlaceholder();
        }
    );
}

function finishLoad(vrm) {
    currentVrm = vrm;
    scene.add(vrm.scene);

    // Center the model
    const box = new THREE.Box3().setFromObject(vrm.scene);
    const center = box.getCenter(new THREE.Vector3());
    const size = box.getSize(new THREE.Vector3());

    // Move model so feet are near y=0
    vrm.scene.position.y = -box.min.y;

    // Adjust camera based on model size
    const maxDim = Math.max(size.x, size.y, size.z);
    camera.position.set(0, size.y * 0.6, maxDim * 2.2);
    camera.lookAt(0, size.y * 0.55, 0);

    console.log("Model added to scene. Size:", size);

    if (window.AndroidBridge) {
        AndroidBridge.modelLoaded(Math.round(size.x * size.y * 1000));
    }
}

function setExpression(name) {
    // simple version
}

function playGesture(name) {}

function setCamera(preset) {
    cameraPreset = preset;
}

function startSimulatedSpeech() {
    isSpeaking = true;
}

function stopSpeech() {
    isSpeaking = false;
}

function speakAudio() {
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

// Expose to Android
window.loadModel = loadModel;
window.setExpression = setExpression;
window.playGesture = playGesture;
window.setCamera = setCamera;
window.startSimulatedSpeech = startSimulatedSpeech;
window.stopSpeech = stopSpeech;
window.speakAudio = speakAudio;

window.onerror = function(msg, url, line) {
    console.error("JS Error:", msg, line);
    if (window.AndroidBridge) {
        AndroidBridge.modelError("JS Error: " + msg);
    }
};