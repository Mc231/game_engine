#version 330 core
out vec4 FragColor;
uniform vec3 uTint;
void main() {
    FragColor = vec4(uTint, 1.0);
}
