#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoord;

out vec3 fragPos;
out vec3 normal;
out vec2 texCoord;
out vec4 fragPosLightSpace;

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProjection;
uniform mat4 uLightSpace;

void main() {
    vec4 world = uModel * vec4(aPos, 1.0);
    fragPos = world.xyz;
    normal = mat3(transpose(inverse(uModel))) * aNormal;
    texCoord = aTexCoord;
    fragPosLightSpace = uLightSpace * world;   // position as the light sees it
    gl_Position = uProjection * uView * world;
}
