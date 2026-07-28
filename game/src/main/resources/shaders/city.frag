#version 330 core
in vec3 vWorld;
in vec3 vNormal;
out vec4 FragColor;

// World-projected (biplanar) texturing so one texture tiles correctly over box
// buildings/sidewalks of any size, with a directional light + a per-batch tint.
uniform sampler2D uTexture;
uniform vec3 uTint;
uniform vec3 uLightDir;     // direction the light travels
uniform vec3 uLightColor;
uniform float uAmbient;
uniform float uTexScale;

void main() {
    vec3 N = normalize(vNormal);

    vec2 uv;
    if (abs(N.y) > 0.5) {
        uv = vWorld.xz * uTexScale;          // roof / ground-facing
    } else if (abs(N.x) > abs(N.z)) {
        uv = vWorld.zy * uTexScale;          // wall facing ±X
    } else {
        uv = vWorld.xy * uTexScale;          // wall facing ±Z
    }

    vec3 base = texture(uTexture, uv).rgb * uTint;
    float d = max(dot(N, normalize(-uLightDir)), 0.0);
    float light = uAmbient + (1.0 - uAmbient) * d;
    FragColor = vec4(base * light * uLightColor, 1.0);
}
