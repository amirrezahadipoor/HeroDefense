#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform vec2 u_texelSize;
uniform vec4 u_glowColor;
uniform float u_time;
uniform float u_intensity;

// Phase 52: double halo + tiny particle specks for stunning legendary/mythic glow
void main() {
    vec4 base = texture2D(u_texture, v_texCoords) * v_color;

    // Inner halo - 1 texel (tight)
    float innerAlpha = 0.0;
    innerAlpha = max(innerAlpha, texture2D(u_texture, v_texCoords + vec2( u_texelSize.x, 0.0)).a);
    innerAlpha = max(innerAlpha, texture2D(u_texture, v_texCoords + vec2(-u_texelSize.x, 0.0)).a);
    innerAlpha = max(innerAlpha, texture2D(u_texture, v_texCoords + vec2(0.0,  u_texelSize.y)).a);
    innerAlpha = max(innerAlpha, texture2D(u_texture, v_texCoords + vec2(0.0, -u_texelSize.y)).a);
    innerAlpha = max(innerAlpha, texture2D(u_texture, v_texCoords + vec2( u_texelSize.x,  u_texelSize.y)).a);
    innerAlpha = max(innerAlpha, texture2D(u_texture, v_texCoords + vec2(-u_texelSize.x,  u_texelSize.y)).a);
    innerAlpha = max(innerAlpha, texture2D(u_texture, v_texCoords + vec2( u_texelSize.x, -u_texelSize.y)).a);
    innerAlpha = max(innerAlpha, texture2D(u_texture, v_texCoords + vec2(-u_texelSize.x, -u_texelSize.y)).a);

    // Outer halo - 2.5 texels (broader, softer)
    float outerAlpha = 0.0;
    float ox = u_texelSize.x * 2.5;
    float oy = u_texelSize.y * 2.5;
    outerAlpha = max(outerAlpha, texture2D(u_texture, v_texCoords + vec2( ox, 0.0)).a);
    outerAlpha = max(outerAlpha, texture2D(u_texture, v_texCoords + vec2(-ox, 0.0)).a);
    outerAlpha = max(outerAlpha, texture2D(u_texture, v_texCoords + vec2(0.0,  oy)).a);
    outerAlpha = max(outerAlpha, texture2D(u_texture, v_texCoords + vec2(0.0, -oy)).a);
    outerAlpha = max(outerAlpha, texture2D(u_texture, v_texCoords + vec2( ox,  oy)).a);
    outerAlpha = max(outerAlpha, texture2D(u_texture, v_texCoords + vec2(-ox,  oy)).a);
    outerAlpha = max(outerAlpha, texture2D(u_texture, v_texCoords + vec2( ox, -oy)).a);
    outerAlpha = max(outerAlpha, texture2D(u_texture, v_texCoords + vec2(-ox, -oy)).a);

    float innerEdge = max(0.0, innerAlpha - base.a);
    float outerEdge = max(0.0, outerAlpha - base.a) * 0.6; // outer softer

    // Dual pulse: inner faster, outer slower
    float innerPulse = 0.85 + 0.15 * sin(u_time * 3.8);
    float outerPulse = 0.75 + 0.25 * sin(u_time * 2.1 + 1.3);

    float innerGlow = innerEdge * u_intensity * innerPulse;
    float outerGlow = outerEdge * u_intensity * 0.7 * outerPulse;

    // Tiny particle specks — deterministic sparkle like reference
    float sparkle = 0.0;
    // Only on glow area
    if (innerEdge > 0.01 || outerEdge > 0.01) {
        float sx = v_texCoords.x * 80.0 + u_time * 1.7;
        float sy = v_texCoords.y * 60.0 + u_time * 1.3;
        float n = sin(sx) * cos(sy);
        float speck = step(0.92, fract(n * 12.9898));
        // Speck flicker
        float flicker = 0.5 + 0.5 * sin(u_time * 7.0 + sx * 0.3);
        sparkle = speck * flicker * 0.8 * u_intensity;
    }

    vec3 glowRgb = u_glowColor.rgb * (innerGlow + outerGlow) + vec3(1.0, 1.0, 0.8) * sparkle;
    float glowAlpha = max(innerGlow, outerGlow) + sparkle * 0.5;

    gl_FragColor = vec4(base.rgb + glowRgb, max(base.a, glowAlpha));
}
