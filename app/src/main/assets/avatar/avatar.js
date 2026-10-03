let scene, camera, renderer, currentVrm;
let cameraPreset = 'fullBody';
let currentExpression = 'neutral';
let activeGesture = null;
let gestureTimer = null;
let isSpeaking = false;
let speechSimInterval = null;

function initScene() {
    const container = document.getElementById('canvas-container');
    if (!container) return;

    if (typeof THREE === 'undefined') {
        showPlaceholder();
        if (window.AndroidBridge) AndroidBridge.modelError("3D Graphics engine loaded in fallback mode.");
        return;
    }

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

function loadModel(vrmUrl) {
    if (typeof THREE === 'undefined' || typeof THREE.VRM === 'undefined') {
        showPlaceholder();
        if (window.AndroidBridge) AndroidBridge.modelError("VRM Loader engine in fallback mode.");
        return;
    }
}

function showPlaceholder() {
    const canvasContainer = document.getElementById('canvas-container');
    const placeholderContainer = document.getElementById('placeholder-container');
    if (canvasContainer) canvasContainer.style.display = 'none';
    if (placeholderContainer) placeholderContainer.style.display = 'flex';
}

function setExpression(name) {
    currentExpression = name;
}

function playGesture(name) {
    activeGesture = name;
}

function setCamera(preset) {
    cameraPreset = preset;
    updateCameraPosition();
}

function startSimulatedSpeech() {
    isSpeaking = true;
}

function stopSpeech() {
    isSpeaking = false;
}

function speakAudio(base64Audio) {
    startSimulatedSpeech();
}

window.onerror = function(msg, url, line) {
    if (window.AndroidBridge) {
        AndroidBridge.modelError("JS Error: " + msg + " at line " + line);
    }
};
