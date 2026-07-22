#version 330 core
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;
layout(location = 3) in vec3 aTangent;

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProjection;

out mat3 TBN;
out vec3 fragPos;
out vec2 texCoord;

void main() {
    vec4 world = uModel * vec4(aPos, 1.0);
    fragPos = world.xyz;
    texCoord = aTexCoord;

    vec3 N = normalize(mat3(transpose(inverse(uModel))) * aNormal);
    vec3 T = normalize(mat3(uModel) * aTangent);
    T = normalize(T - dot(T, N) * N);
    vec3 B = cross(N, T);
    TBN = mat3(T, B, N);

    gl_Position = uProjection * uView * world;
}
