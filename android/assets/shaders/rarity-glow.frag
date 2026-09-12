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

void main() {
    vec4 base = texture2D(u_texture, v_texCoords) * v_color;
    float nearbyAlpha = 0.0;
    nearbyAlpha = max(nearbyAlpha, texture2D(u_texture, v_texCoords + vec2( u_texelSize.x, 0.0)).a);
    nearbyAlpha = max(nearbyAlpha, texture2D(u_texture, v_texCoords + vec2(-u_texelSize.x, 0.0)).a);
    nearbyAlpha = max(nearbyAlpha, texture2D(u_texture, v_texCoords + vec2(0.0,  u_texelSize.y)).a);
    nearbyAlpha = max(nearbyAlpha, texture2D(u_texture, v_texCoords + vec2(0.0, -u_texelSize.y)).a);
    nearbyAlpha = max(nearbyAlpha, texture2D(u_texture, v_texCoords + vec2( u_texelSize.x,  u_texelSize.y)).a);
    nearbyAlpha = max(nearbyAlpha, texture2D(u_texture, v_texCoords + vec2(-u_texelSize.x,  u_texelSize.y)).a);
    nearbyAlpha = max(nearbyAlpha, texture2D(u_texture, v_texCoords + vec2( u_texelSize.x, -u_texelSize.y)).a);
    nearbyAlpha = max(nearbyAlpha, texture2D(u_texture, v_texCoords + vec2(-u_texelSize.x, -u_texelSize.y)).a);

    float exteriorEdge = max(0.0, nearbyAlpha - base.a);
    float pulse = 0.82 + 0.18 * sin(u_time * 3.2);
    float glowAlpha = exteriorEdge * u_intensity * pulse;
    vec3 glowRgb = u_glowColor.rgb * glowAlpha;
    gl_FragColor = vec4(base.rgb + glowRgb, max(base.a, glowAlpha));
}
