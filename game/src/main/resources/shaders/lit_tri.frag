#version 330 core
in vec3 fragPos;
in vec3 normal;
out vec4 FragColor;

#define MAX_LIGHTS 8
struct Light {
    int type;
    vec3 position;
    vec3 direction;
    vec3 color;
    float constant;
    float linear;
    float quadratic;
    float cutOff;
    float outerCutOff;
};
uniform Light uLights[MAX_LIGHTS];
uniform int uLightCount;

uniform vec3 uViewPos;
uniform vec3 uTint;
uniform float uAmbientStrength;
uniform float uSpecularStrength;
uniform float uShininess;
uniform sampler2D uTexture;
uniform sampler2D uNormalMap;
uniform float uTexScale;

vec3 calcLight(Light light, vec3 N, vec3 viewDir) {
    vec3 lightDir;
    float attenuation = 1.0;
    float intensity = 1.0;
    if (light.type == 0) {
        lightDir = normalize(-light.direction);
    } else {
        vec3 toLight = light.position - fragPos;
        float dist = length(toLight);
        lightDir = toLight / dist;
        attenuation = 1.0 / (light.constant + light.linear * dist + light.quadratic * dist * dist);
        if (light.type == 2) {
            float theta = dot(lightDir, normalize(-light.direction));
            float epsilon = light.cutOff - light.outerCutOff;
            intensity = clamp((theta - light.outerCutOff) / epsilon, 0.0, 1.0);
        }
    }
    float diff = max(dot(N, lightDir), 0.0);
    vec3 reflectDir = reflect(-lightDir, N);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), uShininess);
    return (diff * light.color + uSpecularStrength * spec * light.color) * attenuation * intensity;
}

void main() {
    vec3 gN = normalize(normal);

    // World-projected (biplanar) UV + tangent, chosen by the dominant axis,
    // so axis-aligned walls and floors tile the texture correctly.
    vec2 uv;
    vec3 T;
    float s = uTexScale;
    if (abs(gN.y) > 0.5) {          // floor / ceiling
        uv = fragPos.xz * s;
        T = vec3(1.0, 0.0, 0.0);
    } else if (abs(gN.x) > abs(gN.z)) {   // wall facing +/-X
        uv = fragPos.zy * s;
        T = vec3(0.0, 0.0, 1.0);
    } else {                         // wall facing +/-Z
        uv = fragPos.xy * s;
        T = vec3(1.0, 0.0, 0.0);
    }

    vec3 baseColor = texture(uTexture, uv).rgb * uTint;

    // Tangent-space normal → world space.
    vec3 nT = texture(uNormalMap, uv).rgb * 2.0 - 1.0;
    T = normalize(T - gN * dot(T, gN));
    vec3 B = cross(gN, T);
    vec3 N = normalize(T * nT.x + B * nT.y + gN * nT.z);
    N = normalize(mix(gN, N, 0.6));   // normal-map strength

    vec3 viewDir = normalize(uViewPos - fragPos);
    vec3 lighting = vec3(uAmbientStrength);
    for (int i = 0; i < uLightCount; i++) {
        lighting += calcLight(uLights[i], N, viewDir);
    }
    FragColor = vec4(lighting * baseColor, 1.0);
}
