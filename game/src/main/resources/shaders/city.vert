#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aUv;
layout (location = 3) in mat4 aInstance;

out vec3 vWorld;
out vec3 vNormal;

uniform mat4 uView;
uniform mat4 uProjection;

void main() {
    vec4 world = aInstance * vec4(aPos, 1.0);
    vWorld = world.xyz;
    vNormal = mat3(aInstance) * aNormal;
    gl_Position = uProjection * uView * world;
}
