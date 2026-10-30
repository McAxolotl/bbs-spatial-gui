#version 150

/*
Dual Kawase blur adapted from https://github.com/quaIett/bbs-refreshed-addon
Revision: 81096f77392d62432e171209a61e8e05a4763a84
Applies to InterfaceBlur.java and bbs_kawase_* shaders.

MIT License

Copyright (c) 2026 quaIett

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

McAxolotl modification: filter RGBA to preserve transparent GUI coverage.
*/

// Dual Kawase upsample (Bjorge, ARM 2015): four taps a whole source texel out along the axes (weight 1)
// and four diagonal taps half a texel out (weight 2), written into a target twice the size.

uniform sampler2D DiffuseSampler;
uniform float Offset;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

void main() {
    vec2 h = oneTexel * 0.5 * Offset;

    vec4 sum = texture(DiffuseSampler, texCoord + vec2(-h.x * 2.0, 0.0));
    sum += texture(DiffuseSampler, texCoord + vec2(h.x * 2.0, 0.0));
    sum += texture(DiffuseSampler, texCoord + vec2(0.0, -h.y * 2.0));
    sum += texture(DiffuseSampler, texCoord + vec2(0.0, h.y * 2.0));
    sum += texture(DiffuseSampler, texCoord + vec2(-h.x, h.y)) * 2.0;
    sum += texture(DiffuseSampler, texCoord + vec2(h.x, h.y)) * 2.0;
    sum += texture(DiffuseSampler, texCoord + vec2(h.x, -h.y)) * 2.0;
    sum += texture(DiffuseSampler, texCoord + vec2(-h.x, -h.y)) * 2.0;

    fragColor = sum / 12.0;
}
