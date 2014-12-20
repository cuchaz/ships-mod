#version 130

uniform sampler2D texture;

void main( void )
{
	gl_FragColor.rgb = vec3( 1.0, 0.0, 0.0 );
	
	vec4 textureColor = texture2D( texture, gl_TexCoord[0].xy );
	vec4 outlineColor = vec4( 1.0, 0.0, 0.0, 1.0 );
	vec4 ignoreColor = vec4( 0.0, 0.0, 0.0, 0.0 );
	
	ivec2 textureSize = textureSize( texture, 0 );
	
	bool amIOpaque = textureColor.a == 1.0;
	bool isNorthOpaque = texture2D( texture, gl_TexCoord[0].xy - vec2( 0.0, 1.0/textureSize.y ) ).a == 1.0;
	bool isSouthOpaque = texture2D( texture, gl_TexCoord[0].xy + vec2( 0.0, 1.0/textureSize.y ) ).a == 1.0;
	bool isEastOpaque = texture2D( texture, gl_TexCoord[0].xy + vec2( 1.0/textureSize.x, 0.0 ) ).a == 1.0;
	bool isWestOpaque = texture2D( texture, gl_TexCoord[0].xy - vec2( 1.0/textureSize.x, 0.0 ) ).a == 1.0;
	
	if( amIOpaque )
	{
		gl_FragColor = textureColor;
	}
	else if( isNorthOpaque || isSouthOpaque || isEastOpaque || isWestOpaque )
	{
		gl_FragColor = outlineColor;
	}
	else
	{
		gl_FragColor = ignoreColor;
	}
}
