#version 300 es
precision highp float;
precision mediump sampler2D;
uniform sampler2D RawBuffer;
uniform sampler2D GreenBuffer;
uniform sampler2D GainMap;
uniform int yOffset;
uniform int CfaPattern;
uniform vec3 whitePoint;


//#define greenmin (0.04)
#define greenmin (0.01)
//#define greenmax (0.9)
#define greenmax (0.99)
#import interpolation
out vec3 Output;
float interpolateColor(in ivec2 coords){
    bool usegreen = true;
    float green[5];
    green[0] = float(texelFetch(GreenBuffer, (coords+ivec2(-1,-1)), 0).x);
    green[1] = float(texelFetch(GreenBuffer, (coords+ivec2(1,-1)),  0).x);
    green[2] = float(texelFetch(GreenBuffer, (coords+ivec2(-1,1)),  0).x);
    green[3] = float(texelFetch(GreenBuffer, (coords+ivec2(1,1)),   0).x);
    green[4] = float(texelFetch(GreenBuffer, (coords),              0).x);
    for(int i = 0; i<5; i++)if(green[i] < greenmin || green[i] > greenmax) usegreen = false;
    if(usegreen){
        float coeff[4];
        coeff[0] = float(texelFetch(RawBuffer, (coords+ivec2(-1,-1)), 0).x)/(green[0]);
        coeff[1] = float(texelFetch(RawBuffer, (coords+ivec2(1,-1)),  0).x)/(green[1]);
        coeff[2] = float(texelFetch(RawBuffer, (coords+ivec2(-1,1)),  0).x)/(green[2]);
        coeff[3] = float(texelFetch(RawBuffer, (coords+ivec2(1,1)),   0).x)/(green[3]);
        return (green[4]*(coeff[0]+coeff[1]+coeff[2]+coeff[3])/4.);
    } else {
        return ((float(texelFetch(RawBuffer, (coords+ivec2(-1,-1)), 0).x)+float(texelFetch(RawBuffer, (coords+ivec2(1,-1)), 0).x)
        +float(texelFetch(RawBuffer, (coords+ivec2(-1,1)), 0).x)+float(texelFetch(RawBuffer, (coords+ivec2(1,1)), 0).x))/(4.));
        }
}
float interpolateColorx(in ivec2 coords){
    bool usegreen = true;
    float green[3];
    green[0] = float(texelFetch(GreenBuffer, (coords+ivec2(-1,0)), 0).x);
    green[1] = float(texelFetch(GreenBuffer, (coords+ivec2(0,0)),  0).x);
    green[2] = float(texelFetch(GreenBuffer, (coords+ivec2(1,0)),  0).x);
    for(int i = 0; i<3; i++)if(green[i] < greenmin || green[i] > greenmax) usegreen = false;
    if(usegreen){
        float coeff[2];
        coeff[0] = float(texelFetch(RawBuffer, (coords+ivec2(-1,0)), 0).x)/(green[0]);
        coeff[1] = float(texelFetch(RawBuffer, (coords+ivec2(1,0)),  0).x)/(green[2]);
        return (green[1]*(coeff[0]+coeff[1])/2.);
    } else {
        return ((float(texelFetch(RawBuffer, (coords+ivec2(-1,0)), 0).x)+float(texelFetch(RawBuffer, (coords+ivec2(1,0)), 0).x))/(2.));
    }
}
float interpolateColory(in ivec2 coords){
    bool usegreen = true;
    float green[3];
    green[0] = float(texelFetch(GreenBuffer, (coords+ivec2(0,-1)), 0).x);
    green[1] = float(texelFetch(GreenBuffer, (coords+ivec2(0,0)),  0).x);
    green[2] = float(texelFetch(GreenBuffer, (coords+ivec2(0,1)),  0).x);
    for(int i = 0; i<3; i++)if(green[i] < greenmin || green[i] > greenmax) usegreen = false;
    if(usegreen){
        float coeff[2];
        coeff[0] = float(texelFetch(RawBuffer, (coords+ivec2(0,-1)), 0).x)/(green[0]);
        coeff[1] = float(texelFetch(RawBuffer, (coords+ivec2(0,1)),  0).x)/(green[2]);
        return (green[1]*(coeff[0]+coeff[1])/2.);
    } else {
        return ((float(texelFetch(RawBuffer, (coords+ivec2(0,-1)), 0).x)+float(texelFetch(RawBuffer, (coords+ivec2(0,1)), 0).x))/(2.));
    }
}
void main() {
    ivec2 xy = ivec2(gl_FragCoord.xy);
    int fact1 = xy.x%2;
    int fact2 = xy.y%2;
    xy+=ivec2(0,yOffset);
    vec3 outp;
    if(fact1 ==0 && fact2 == 0) {//rggb
        outp.g = texelFetch(GreenBuffer, (xy), 0).x;
        outp.r = float(texelFetch(RawBuffer, (xy), 0).x);
        outp.b = interpolateColor(xy);
    } else
    if(fact1 ==1 && fact2 == 0) {//grbg
        outp.g = texelFetch(GreenBuffer, (xy), 0).x;
        //outp.r =((float(texelFetch(RawBuffer, (xy+ivec2(-1,0)), 0).x)+float(texelFetch(RawBuffer, (xy+ivec2(1,0)), 0).x))/(2.*float(WhiteLevel)));
        outp.r = interpolateColorx(xy);
        //outp.b =((float(texelFetch(RawBuffer, (xy+ivec2(0,-1)), 0).x)+float(texelFetch(RawBuffer, (xy+ivec2(0,1)), 0).x))/(2.*float(WhiteLevel)));
        outp.b = interpolateColory(xy);

    } else
    if(fact1 ==0 && fact2 == 1) {//gbrg
        outp.g = texelFetch(GreenBuffer, (xy), 0).x;
        //outp.b =((float(texelFetch(RawBuffer, (xy+ivec2(-1,0)), 0).x)+float(texelFetch(RawBuffer, (xy+ivec2(1,0)), 0).x))/(2.*float(WhiteLevel)));
        outp.b = interpolateColorx(xy);
        //outp.r =((float(texelFetch(RawBuffer, (xy+ivec2(0,-1)), 0).x)+float(texelFetch(RawBuffer, (xy+ivec2(0,1)), 0).x))/(2.*float(WhiteLevel)));
        outp.r = interpolateColory(xy);

    } else  {//bggr
        outp.g = texelFetch(GreenBuffer, (xy), 0).x;
        outp.b = float(texelFetch(RawBuffer, (xy), 0).x);
        outp.r = interpolateColor(xy);
    }
    Output = outp;
    vec4 gains = textureBicubicHardware(GainMap, vec2(gl_FragCoord.xy)/vec2(textureSize(GreenBuffer, 0)));
    vec3 neutralScaled = min(min(gains.x, gains.y), min(gains.z, gains.w)) * whitePoint;
    vec3 npf = Output / neutralScaled;
    Output = min(Output, neutralScaled);
    // When both red and blue channels are above white point, assume green is too
    // So extend dynamic range by scaling white point
    // Use a bias so only high green values become higher
    // In highlights, bias should be one
    float bias = npf.g * npf.g * npf.g;
    Output *= mix(1.f, max(npf.r + npf.b, 2.f) * 0.5f, bias);
    Output = clamp(Output,0.0,1.0);
}