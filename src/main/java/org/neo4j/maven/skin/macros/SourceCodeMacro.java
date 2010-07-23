package org.neo4j.maven.skin.macros;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.maven.doxia.macro.AbstractMacro;
import org.apache.maven.doxia.macro.MacroExecutionException;
import org.apache.maven.doxia.macro.MacroRequest;
import org.apache.maven.doxia.sink.Sink;

/**
 * @author Tobias Ivarsson
 * @plexus.component role="org.apache.maven.doxia.macro.Macro" role-hint="source-code"
 */
public class SourceCodeMacro extends AbstractMacro
{
    private static final Pattern START_SNIPPET = Pattern.compile( "START SNIPPET:\\s*(\\S+)" );
    private static final Pattern END_SNIPPET = Pattern.compile( "END SNIPPET:\\s*(\\S+)" );
    private static final String newline = System.getProperty( "line.separator" );

    private static String getBrush( String suffix )
    {
        return suffix;
    }

    private static boolean match( String line, Pattern pattern, String id )
    {
        Matcher match = pattern.matcher( line );
        return match.find() && id.equals( match.group( 1 ) );
    }

    static class SourceCode
    {
        private final String brush;
        private final BufferedReader reader;

        SourceCode( URI uri ) throws MacroExecutionException
        {
            try
            {
                reader = new BufferedReader( new InputStreamReader(
                        uri.toURL().openStream() ) );
            }
            catch ( Exception e )
            {
                throw new MacroExecutionException( "Could not open file", e );
            }
            String path = uri.getPath();
            brush = getBrush( path.substring( path.lastIndexOf( '.' ) + 1 ) );
        }

        private void emit( Sink sink, String content )
        {
            sink.rawText( "<div class=\"source\">" );
            if ( brush == null )
            {
                sink.rawText( "<pre>" );
            }
            else
            {
                sink.rawText( "<pre class=\"brush: " + brush + "\">" );
            }
            sink.rawText( StringEscapeUtils.escapeHtml( content ) );
            sink.rawText( "</pre></div>" );
        }

        private String readLine() throws MacroExecutionException
        {
            try
            {
                return reader.readLine();
            }
            catch ( IOException e )
            {
                throw new MacroExecutionException(
                        "Could not read from file", e );
            }
        }

        private String readAll() throws MacroExecutionException
        {
            StringBuilder result = new StringBuilder();
            String line;
            try
            {
                while ( ( line = reader.readLine() ) != null )
                    result.append( line ).append( newline );
            }
            catch ( IOException e )
            {
                throw new MacroExecutionException(
                        "Could not read from file", e );
            }
            return result.toString();
        }

        void emitFile( Sink sink ) throws MacroExecutionException
        {
            emit( sink, readAll() );
        }

        void emitSnippet( String id, Sink sink ) throws MacroExecutionException
        {
            emit( sink, findSnippet( id ) );
        }

        private String findSnippet( String id ) throws MacroExecutionException
        {
            String line;
            while ( ( line = readLine() ) != null )
                if ( match( line, START_SNIPPET, id ) ) break;
            if ( line != null )
            {
                StringBuilder result = new StringBuilder();
                while ( ( line = readLine() ) != null )
                {
                    if ( match( line, END_SNIPPET, id ) )
                    {
                        return result.toString();
                    }
                    result.append( line ).append( newline );
                }
            }
            throw new MacroExecutionException(
                    "Could not find snippet \"" + id + "\"" );
        }
    }

    public void execute( Sink sink, MacroRequest request )
            throws MacroExecutionException
    {
        SourceCode code = new SourceCode( getUri( request ) );
        final String snippet;
        try
        {
            snippet = (String) request.getParameter( "snippet" );
        }
        catch ( ClassCastException ex )
        {
            throw new MacroExecutionException( "Illegally formatted snippet id" );
        }
        if ( snippet == null )
        {
            code.emitFile( sink );
        }
        else
        {
            code.emitSnippet( snippet, sink );
        }
    }

    private URI getUri( MacroRequest request ) throws MacroExecutionException
    {
        Object uri = request.getParameter( "uri" );
        Object file = request.getParameter( "file" );
        if ( ( uri == null && file == null ) || ( uri != null && file != null ) )
        {
            throw new MacroExecutionException(
                    "Exectly one of the parameters \"uri\" and "
                            + "\"file\" must be specified." );
        }
        else if ( uri != null )
        {
            try
            {
                return new URI( (String) uri );
            }
            catch ( Exception e )
            {
                throw new MacroExecutionException(
                        "Illegally formatted URI (uri parameter).", e );
            }
        }
        else
        {
            try
            {
                File path = new File( (String) file );
                if ( !path.isAbsolute() )
                {
                    path = new File( request.getBasedir(), (String) file );
                }
                if ( !path.exists() )
                {
                    throw new MacroExecutionException( "No such file: \""
                                                       + file + "\"" );
                }
                return path.toURI();
            }
            catch ( Exception e )
            {
                throw new MacroExecutionException(
                        "Illegally formatted URI (file parameter).", e );
            }
        }
    }
}
