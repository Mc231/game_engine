#version 330 core
in vec3 fragPos;
in vec3 normal;
in vec2 texCoord;
out vec4 FragColor;

#define MAX_LIGHTS 8
struct Light {
    int type;              // 0 = directional, 1 = point, 2 = spot
    vec3 position;
    vec3 direction;
    vec3 color;
    float constant;
    float linear;
    float quadratic;
    float cutOff;          // cos(inner angle)
    float outerCutOff;     // cos(outer angle)
};
uniform Light uLights[MAX_LIGHTS];
uniform int uLightCount;

uniform sampler2D uTexture;
uniform vec3 uViewPos;
uniform vec3 uTint;
uniform float uAmbientStrength;
uniform float uSpecularStrength;
uniform float uShininess;

// Distance fog (uFogDensity 0 = disabled, so scenes that don't set it are unaffected).
uniform vec3 uFogColor;
uniform float uFogDensity;

vec3 calcLight(Light light, vec3 N, vec3 viewDir) {
    vec3 lightDir;
    float attenuation = 1.0;
    float intensity = 1.0;

    if (light.type == 0) {                 // directional
        lightDir = normalize(-light.direction);
    } else {                               // point or spot
        vec3 toLight = light.position - fragPos;
        float dist = length(toLight);
        lightDir = toLight / dist;
        attenuation = 1.0 / (light.constant + light.linear * dist
                             + light.quadratic * dist * dist);
        if (light.type == 2) {             // spot cone
            float theta = dot(lightDir, normalize(-light.direction));
            float epsilon = light.cutOff - light.outerCutOff;
            intensity = clamp((theta - light.outerCutOff) / epsilon, 0.0, 1.0);
        }
    }

    float diff = max(dot(N, lightDir), 0.0);
    vec3 reflectDir = reflect(-lightDir, N);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), uShininess);

    vec3 diffuse = diff * light.color;
    vec3 specular = uSpecularStrength * spec * light.color;
    return (diffuse + specular) * attenuation * intensity;
}

void main() {
    vec3 baseColor = texture(uTexture, texCoord).rgb * uTint;
    vec3 N = normalize(normal);
    vec3 viewDir = normalize(uViewPos - fragPos);

    vec3 lighting = vec3(uAmbientStrength);
    for (int i = 0; i < uLightCount; i++) {
        lighting += calcLight(uLights[i], N, viewDir);
    }
    vec3 color = lighting * baseColor;

    // Exponential distance fog toward uFogColor.
    if (uFogDensity > 0.0) {
        float dist = length(uViewPos - fragPos);
        float fog = 1.0 - exp(-dist * uFogDensity);
        color = mix(color, uFogColor, clamp(fog, 0.0, 1.0));
    }
    FragColor = vec4(color, 1.0);
}
