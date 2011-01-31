package org.neo4j.maven.skin.macros;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;

import org.apache.maven.doxia.macro.MacroExecutionException;
import org.apache.maven.doxia.sink.Sink;
import org.junit.Before;
import org.junit.Test;

public class TestSourceCodeMacro
{
    private SourceCodeMacro.SourceCode code;

    @Test
    public void canIncludeFile() throws Exception
    {
        StringBuilder result = new StringBuilder();
        code.emitFile( sink( result ) );
        String output = result.toString();
        int length = verifyHtml( output, "java" );
        assertTrue( "Missing output from file", length > 100 );
    }

    @Test
    public void canIncludeSnippet() throws Exception
    {
        StringBuilder result = new StringBuilder();
        // START SNIPPET: test
        code.emitSnippet( "test", sink( result ) );
        // END SNIPPET: test
        String output = result.toString();
        verifyHtml( output, "java",
                "code.emitSnippet( &quot;test&quot;, sink( result ) );" );
    }

    @Test( expected = MacroExecutionException.class )
    public void cannotOpenNonExistentFile() throws Exception
    {
        new SourceCodeMacro.SourceCode( new URI( "http://void" ) );
    }

    static int verifyHtml( String output, String brush, String... lines )
            throws Exception
    {
        final String prefix;
        final String suffix = "</pre></div>";
        if ( brush == null )
        {
            prefix = "<div class=\"source\"><pre>";
        }
        else
        {
            prefix = "<div class=\"source\"><pre class=\"brush: " + brush + "\">";
        }
        assertTrue( output.startsWith( prefix ) );
        assertTrue( output.endsWith( suffix ) );
        final String content = output.substring( prefix.length(),
                output.length() - suffix.length() );
        if ( lines != null && lines.length != 0 )
        {
            BufferedReader reader = new BufferedReader( new StringReader( content ) );
            for ( String line : lines )
            {
                String readLine = reader.readLine();
                assertNotNull( "Not enough lines in output", readLine );
                assertEquals( line.trim(), readLine.trim() );
            }
            assertNull( "Too many lines in input", reader.readLine() );
        }
        return content.length();
    }

    private Sink sink( final StringBuilder result ) throws Exception
    {
        final Method ok = Sink.class.getMethod( "rawText", String.class );
        return (Sink) Proxy.newProxyInstance( Sink.class.getClassLoader(),
                new Class[] { Sink.class }, new InvocationHandler()
                {
                    public Object invoke( Object proxy, Method method,
                            Object[] args ) throws Throwable
                    {
                        if ( method.equals( ok ) )
                        {
                            result.append( args[0] );
                        }
                        else
                        {
                            throw new UnsupportedOperationException(
                                    method.toString() );
                        }
                        return null;
                    }
                } );
    }

    @Before
    public void createCode() throws Exception
    {
        code = new SourceCodeMacro.SourceCode( sourceFile( getClass() ) );
    }

    private static URI sourceFile( Class<?> cls ) throws Exception
    {
        File path = new File(
                cls.getProtectionDomain().getCodeSource().getLocation().toURI() );
        path = new File( path.getParentFile().getParentFile(), "src/test/java" );
        path = new File( path, cls.getName().replace( '.', '/' ) + ".java" );
        return path.toURI();
    }
}
