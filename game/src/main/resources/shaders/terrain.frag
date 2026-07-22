#version 330 core
in vec3 fragPos;
in vec3 normal;
in float height;
out vec4 FragColor;

uniform vec3 uLightDir;    // direction the sun travels
uniform vec3 uViewPos;
uniform float uMaxHeight;
uniform vec3 uFogColor;

// Blend terrain colors by normalized elevation and steepness.
vec3 terrainColor(float t, float slope) {
    vec3 sand  = vec3(0.76, 0.70, 0.50);
    vec3 grass = vec3(0.29, 0.52, 0.24);
    vec3 rock  = vec3(0.42, 0.38, 0.35);
    vec3 snow  = vec3(0.95, 0.96, 0.98);

    vec3 c = mix(sand, grass, smoothstep(0.02, 0.12, t));
    c = mix(c, rock, smoothstep(0.32, 0.52, t));
    c = mix(c, snow, smoothstep(0.68, 0.86, t));
    // Steep faces are rocky regardless of height.
    c = mix(c, rock, smoothstep(0.55, 0.8, slope));
    return c;
}

void main() {
    vec3 N = normalize(normal);
    float slope = 1.0 - N.y;                 // 0 = flat, 1 = vertical
    float t = clamp(height / uMaxHeight, 0.0, 1.0);
    vec3 base = terrainColor(t, slope);

    // Directional sun: ambient + diffuse.
    vec3 lightDir = normalize(-uLightDir);
    float diff = max(dot(N, lightDir), 0.0);
    vec3 color = base * (0.35 + 0.8 * diff);

    // Distance fog blends far terrain into the sky (also hides the far plane).
    float dist = length(uViewPos - fragPos);
    float fog = clamp((dist - 300.0) / (750.0 - 300.0), 0.0, 1.0);
    color = mix(color, uFogColor, fog);

    FragColor = vec4(color, 1.0);
}
