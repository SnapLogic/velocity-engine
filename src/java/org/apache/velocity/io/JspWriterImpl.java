package org.apache.velocity.io;

/*
 * $Header: /home/cvs/jakarta-velocity/src/java/org/apache/velocity/io/Attic/JspWriterImpl.java,v 1.3 2000/11/04 04:58:28 jon Exp $
 * $Revision: 1.3 $
 * $Date: 2000/11/04 04:58:28 $
 *
 * ====================================================================
 * 
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */ 

import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Write text to a character-output stream, buffering characters so as
 * to provide for the efficient writing of single characters, arrays,
 * and strings. 
 *
 * Provide support for discarding for the output that has been 
 * buffered. 
 * 
 * This needs revisiting when the buffering problems in the JSP spec
 * are fixed -akv 
 *
 * @author Anil K. Vijendran
 */
public final class JspWriterImpl extends JspWriter
{
    private OutputStreamWriter writer;
    
    private char cb[];
    private int nextChar;

    private static int defaultCharBufferSize = 8 * 1024;

    private boolean flushed = false;

    /**
     * Create a buffered character-output stream that uses a default-sized
     * output buffer.
     *
     * @param  response  A Servlet Response
     */
    public JspWriterImpl(OutputStreamWriter writer)
    {
        this(writer, defaultCharBufferSize, true);
    }

    /**
     * Create a new buffered character-output stream that uses an output
     * buffer of the given size.
     *
     * @param  response A Servlet Response
     * @param  sz   	Output-buffer size, a positive integer
     *
     * @exception  IllegalArgumentException  If sz is <= 0
     */
    public JspWriterImpl(OutputStreamWriter writer, int sz, boolean autoFlush)
    {
        super(sz, autoFlush);
        if (sz < 0)
            throw new IllegalArgumentException("Buffer size <= 0");
        this.writer = writer;
        cb = sz == 0 ? null : new char[sz];
        nextChar = 0;
    }

    private final void init( OutputStreamWriter writer, int sz, boolean autoFlush )
    {
        this.writer= writer;
        if( sz > 0 && ( cb == null || sz > cb.length ) )
            cb=new char[sz];
        nextChar = 0;
        this.autoFlush=autoFlush;
        this.bufferSize=sz;
    }

    /**
     * Flush the output buffer to the underlying character stream, without
     * flushing the stream itself.  This method is non-private only so that it
     * may be invoked by PrintStream.
     */
    private final void flushBuffer() throws IOException
    {
        if (bufferSize == 0)
            return;
        flushed = true;
        if (nextChar == 0)
            return;
        writer.write(cb, 0, nextChar);
        nextChar = 0;
    }

    /**
     * Discard the output buffer.
     */
    public final void clear()
    {
        nextChar = 0;
    }

    private final void bufferOverflow() throws IOException
    {
        throw new IOException("overflow");
    }

    /**
     * Flush the stream.
     *
     */
    public final void flush()  throws IOException
    {
        flushBuffer();
        if (writer != null)
        {
            writer.flush();
        }
    }

    /**
     * Close the stream.
     *
     */
    public final void close() throws IOException {
        if (writer == null)
            return;
        flush();
    }

    /**
     * @return the number of bytes unused in the buffer
     */
    public final int getRemaining()
    {
        return bufferSize - nextChar;
    }

    /**
     * Write a single character.
     *
     */
    public final void write(int c) throws IOException
    {
        if (bufferSize == 0)
        {
            writer.write(c);
        }
        else
        {
            if (nextChar >= bufferSize)
                if (autoFlush)
                    flushBuffer();
                else
                    bufferOverflow();
            cb[nextChar++] = (char) c;
        }
    }

    /**
     * Our own little min method, to avoid loading java.lang.Math if we've run
     * out of file descriptors and we're trying to print a stack trace.
     */
    private final int min(int a, int b)
    {
	    if (a < b) return a;
    	    return b;
    }

    /**
     * Write a portion of an array of characters.
     *
     * <p> Ordinarily this method stores characters from the given array into
     * this stream's buffer, flushing the buffer to the underlying stream as
     * needed.  If the requested length is at least as large as the buffer,
     * however, then this method will flush the buffer and write the characters
     * directly to the underlying stream.  Thus redundant
     * <code>DiscardableBufferedWriter</code>s will not copy data unnecessarily.
     *
     * @param  cbuf  A character array
     * @param  off   Offset from which to start reading characters
     * @param  len   Number of characters to write
     *
     */
    public final void write(char cbuf[], int off, int len) 
        throws IOException 
    {
        if (bufferSize == 0)
        {
            writer.write(cbuf, off, len);
            return;
        }

        if (len == 0)
        {
            return;
        } 

        if (len >= bufferSize)
        {
            /* If the request length exceeds the size of the output buffer,
            flush the buffer and then write the data directly.  In this
            way buffered streams will cascade harmlessly. */
            if (autoFlush)
                flushBuffer();
            else
                bufferOverflow();
                writer.write(cbuf, off, len);
            return;
        }

        int b = off, t = off + len;
        while (b < t)
        {
            int d = min(bufferSize - nextChar, t - b);
            System.arraycopy(cbuf, b, cb, nextChar, d);
            b += d;
            nextChar += d;
            if (nextChar >= bufferSize) 
                if (autoFlush)
                    flushBuffer();
                else
                    bufferOverflow();
        }
    }

    /**
     * Write an array of characters.  This method cannot be inherited from the
     * Writer class because it must suppress I/O exceptions.
     */
    public final void write(char buf[]) throws IOException
    {
    	write(buf, 0, buf.length);
    }

    /**
     * Write a portion of a String.
     *
     * @param  s     String to be written
     * @param  off   Offset from which to start reading characters
     * @param  len   Number of characters to be written
     *
     */
    public final void write(String s, int off, int len) throws IOException
    {
        if (bufferSize == 0)
        {
            writer.write(s, off, len);
            return;
        }
        int b = off, t = off + len;
        while (b < t)
        {
            int d = min(bufferSize - nextChar, t - b);
            s.getChars(b, b + d, cb, nextChar);
            b += d;
            nextChar += d;
            if (nextChar >= bufferSize) 
                if (autoFlush)
                    flushBuffer();
                else
                    bufferOverflow();
        }
    }

    /**
     * Write a string.  This method cannot be inherited from the Writer class
     * because it must suppress I/O exceptions.
     */
    public final void write(String s) throws IOException
    {
    	write(s, 0, s.length());
    }

    /**
     * resets this class so that it can be reused
     *
     */
    public final void recycle(OutputStreamWriter writer)
    {
        this.writer = writer;
        flushed = false;
        clear();
    }
}
