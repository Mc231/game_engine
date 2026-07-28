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

// Distance fog toward the horizon (uFogDensity 0 = off).
uniform vec3 uViewPos;
uniform vec3 uFogColor;
uniform float uFogDensity;

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
    vec3 color = base * light * uLightColor;

    if (uFogDensity > 0.0) {
        float dist = length(uViewPos - vWorld);
        float fog = 1.0 - exp(-dist * uFogDensity);
        color = mix(color, uFogColor, clamp(fog, 0.0, 1.0));
    }
    FragColor = vec4(color, 1.0);
}
