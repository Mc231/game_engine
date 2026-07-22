#version 330 core
in vec3 fragPos;
in vec3 normal;
in vec2 texCoord;
in vec4 fragPosLightSpace;
out vec4 FragColor;

uniform sampler2D uTexture;
uniform sampler2D uShadowMap;
uniform vec3 uLightDir;     // direction the light travels
uniform vec3 uLightColor;
uniform vec3 uViewPos;

// Returns 1.0 = fully shadowed, 0.0 = fully lit (with PCF soft edges).
float shadowFactor(vec4 posLightSpace, vec3 N, vec3 lightDir) {
    vec3 proj = posLightSpace.xyz / posLightSpace.w;   // clip -> NDC
    proj = proj * 0.5 + 0.5;                           // NDC [-1,1] -> [0,1]
    if (proj.z > 1.0) {
        return 0.0;                                    // outside the light frustum: lit
    }
    float currentDepth = proj.z;
    // Slope-scaled bias to fight shadow acne on grazing angles.
    float bias = max(0.0025 * (1.0 - dot(N, lightDir)), 0.0008);

    float shadow = 0.0;
    vec2 texel = 1.0 / textureSize(uShadowMap, 0);
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            float closest = texture(uShadowMap, proj.xy + vec2(x, y) * texel).r;
            shadow += currentDepth - bias > closest ? 1.0 : 0.0;
        }
    }
    return shadow / 9.0;
}

void main() {
    vec3 baseColor = texture(uTexture, texCoord).rgb;
    vec3 N = normalize(normal);
    vec3 lightDir = normalize(-uLightDir);
    vec3 viewDir = normalize(uViewPos - fragPos);

    float diff = max(dot(N, lightDir), 0.0);
    vec3 reflectDir = reflect(-lightDir, N);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);

    float shadow = shadowFactor(fragPosLightSpace, N, lightDir);
    vec3 ambient = 0.25 * uLightColor;
    vec3 lit = (1.0 - shadow) * (diff + 0.4 * spec) * uLightColor;

    FragColor = vec4((ambient + lit) * baseColor, 1.0);
}
