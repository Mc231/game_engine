#version 330 core
in mat3 TBN;
in vec3 fragPos;
in vec2 texCoord;
out vec4 FragColor;

uniform sampler2D uTexture;
uniform sampler2D uNormalMap;
uniform vec3 uLightPos;
uniform vec3 uLightColor;
uniform vec3 uViewPos;

void main() {
    vec3 baseColor = texture(uTexture, texCoord).rgb;

    // Tangent-space normal, transformed to world space.
    vec3 n = texture(uNormalMap, texCoord).rgb * 2.0 - 1.0;
    vec3 N = normalize(TBN * n);

    vec3 lightDir = normalize(uLightPos - fragPos);
    vec3 viewDir = normalize(uViewPos - fragPos);
    vec3 reflectDir = reflect(-lightDir, N);

    vec3 ambient = vec3(0.15);
    vec3 diffuse = max(dot(N, lightDir), 0.0) * uLightColor;
    vec3 specular = pow(max(dot(viewDir, reflectDir), 0.0), 32.0) * 0.5 * uLightColor;

    FragColor = vec4((ambient + diffuse + specular) * baseColor, 1.0);
}
