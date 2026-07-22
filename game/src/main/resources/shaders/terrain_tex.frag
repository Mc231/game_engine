#version 330 core
in vec3 fragPos;
in vec3 normal;
in float height;
out vec4 FragColor;

uniform vec3 uLightDir;
uniform vec3 uViewPos;
uniform float uMaxHeight;
uniform vec3 uFogColor;
uniform sampler2D uGrass;
uniform sampler2D uRock;
uniform sampler2D uGrassN;
uniform sampler2D uRockN;

void main() {
    vec3 N = normalize(normal);
    float slope = 1.0 - N.y;                 // 0 flat, 1 vertical

    // Tile textures by world position (two scales to reduce obvious repetition).
    vec2 uv = fragPos.xz * 0.12;
    vec3 grass = texture(uGrass, uv).rgb;
    vec3 rock  = texture(uRock, uv * 0.5).rgb;

    float t = clamp(height / uMaxHeight, 0.0, 1.0);
    float rockMix = clamp(smoothstep(0.30, 0.6, slope) + smoothstep(0.6, 0.95, t) * 0.6, 0.0, 1.0);
    vec3 base = mix(grass, rock, rockMix);

    // --- Normal mapping: perturb the surface normal for lit detail ---
    // UV maps to world XZ, so build a TBN whose tangent follows world +X.
    vec3 nGrass = texture(uGrassN, uv).rgb * 2.0 - 1.0;
    vec3 nRock  = texture(uRockN, uv * 0.5).rgb * 2.0 - 1.0;
    vec3 nTex = normalize(mix(nGrass, nRock, rockMix));
    vec3 T = normalize(vec3(1.0, 0.0, 0.0) - N * N.x);
    vec3 B = cross(N, T);
    vec3 Np = normalize(T * nTex.x + B * nTex.y + N * nTex.z);
    N = normalize(mix(N, Np, 0.7));          // 0.7 = normal-map strength

    // Directional sun.
    vec3 lightDir = normalize(-uLightDir);
    float diff = max(dot(N, lightDir), 0.0);
    vec3 color = base * (0.4 + 0.8 * diff);

    // Exponential distance fog (matches the rest of the world).
    float dist = length(uViewPos - fragPos);
    float fog = 1.0 - exp(-dist * 0.0016);
    color = mix(color, uFogColor, clamp(fog, 0.0, 1.0));

    FragColor = vec4(color, 1.0);
}
