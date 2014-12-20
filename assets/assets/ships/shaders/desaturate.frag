#version 120

uniform sampler2D texture;

void main( void )
{
	vec4 textureColor = texture2D( texture, gl_TexCoord[0].xy );
	float grey = ( textureColor.r + textureColor.g + textureColor.b )/6.0;
	gl_FragColor = vec4( grey, grey, grey, textureColor.a );
}
