package org.zwen.media.core;

import java.awt.*;

import javax.media.*;
import javax.media.format.VideoFormat;

/**
 * Encapsulates format information for video data. The attributes of a
 * <tt>VideoFormat</tt> include the encoding type, frame size, frame rate, and
 * the data type.
 */
public class H264Format extends VideoFormat
{
	public static final H264Format H264_RTP = new H264Format("h264/RTP");

	public static final H264Format H264 = new H264Format("h264");
	
    /**
     * Constructs a <tt>VideoFormat</tt> with the specified encoding type.
     * 
     * @param encoding
     *            A <tt>String</tt> that describes the encoding type for this
     *            <tt>VideoFormat</tt>.
     */
    public H264Format(String encoding)
    {
        super(encoding);
    }

    /**
     * Constructs a <tt>VideoFormat</tt> with the specified attributes.
     * 
     * @param encoding
     *            A <tt>String</tt> that describes the encoding type for this
     *            <tt>VideoFormat</tt>.
     * @param size
     *            The size of a video frame.
     * @param maxDataLength
     *            The maximum length of a data chunk.
     * @param dataType
     *            The type of data. For example, byte array.
     * @param frameRate
     *            The frame rate.
     */
    public H264Format(String encoding, Dimension size, int maxDataLength,
            Class<?> dataType, float frameRate)
    {
        this(encoding);
        if (size != null)
            this.size = new Dimension(size);
        this.maxDataLength = maxDataLength;
        this.dataType = dataType;
        this.frameRate = frameRate;
    }

    /**
     * Creates a clone of this <tt>VideoFormat</tt> by copying each field to the
     * clone.
     * 
     * @return A clone of this <tt>VideoFormat</tt>.
     */
    @Override
    public Object clone()
    {
        H264Format f = new H264Format(encoding, size, maxDataLength,
                dataType, frameRate);
        f.copy(this);
        return f;
    }

    /**
     * Copies the attributes from the specified <tt>Format</tt> into this
     * <tt>VideoFormat</tt>.
     * 
     * @param f
     *            The <tt>Format</tt> to copy the attributes from.
     */
    @Override
    protected void copy(Format f)
    {
        super.copy(f);
        H264Format vf = (H264Format) f;
        if (vf.size != null)
            size = new Dimension(vf.size);
        maxDataLength = vf.maxDataLength;
        frameRate = vf.frameRate;
    }

    /**
     * Compares the specified <tt>Format</tt> with this <tt>VideoFormat</tt>.
     * Returns <tt>true</tt> only if the specified <tt>Format</tt> is a
     * <tt>VideoFormat</tt> object and all of its attributes are identical to
     * the attributes in this <tt>VideoFormat</tt>.
     * 
     * @param format
     *            The <tt>Format</tt> to compare.
     * @return true if the specified <tt>Format</tt> is the same as this one.
     */
    @Override
    public boolean equals(Object format)
    {
        if (format instanceof H264Format)
        {
            H264Format vf = (H264Format) format;

            if (size == null || vf.size == null)
            {
                if (size != vf.size)
                    return false;
            } else
            {
                if (!size.equals(vf.size))
                    return false;
            }

            return super.equals(format) && maxDataLength == vf.maxDataLength
                    && frameRate == vf.frameRate;
        }
        return false;
    }

    /**
     * Gets the frame rate associated with this <tt>VideoFormat</tt>.
     * 
     * @return The frame rate.
     */
    public float getFrameRate()
    {
        return frameRate;
    }

    /**
     * Gets the length of the largest data chunk associated with this
     * <tt>VideoFormat</tt>.
     * 
     * @return The maximum length of a data chunk in this <tt>VideoFormat</tt>.
     */
    public int getMaxDataLength()
    {
        return maxDataLength;
    }

    /**
     * Gets the dimensions of a video frame in this <tt>VideoFormat</tt>.
     * 
     * @return A <tt>Dimension</tt> that specifies the frame size.
     */
    public Dimension getSize()
    {
        return size;
    }

    /**
     * Finds the attributes shared by two matching <tt>Format</tt> objects. If
     * the specified <tt>Format</tt> does not match this one, the result is
     * undefined.
     * 
     * @param format The matching <tt>Format</tt> to intersect with this
     * <tt>VideoFormat</tt>.
     * @return A <tt>Format</tt> object with its attributes set to those
     * attributes common to both <tt>Format</tt> objects.
     * @see #matches
     */
    @Override
    public Format intersects(Format format)
    {
        Format fmt;
        if ((fmt = super.intersects(format)) == null)
            return null;
        if (!(format instanceof H264Format))
            return fmt;
        H264Format other = (H264Format) format;
        H264Format res = (H264Format) fmt;
        res.size = (size != null ? size : other.size);
        res.maxDataLength = (maxDataLength != NOT_SPECIFIED ? maxDataLength
                : other.maxDataLength);
        res.frameRate = (frameRate != NOT_SPECIFIED ? frameRate
                : other.frameRate);
        return res;
    }

    /**
     * Checks whether or not the specified <tt>Format</tt> <EM>matches</EM> this
     * <tt>VideoFormat</tt>. Matches only compares the attributes that are
     * defined in the specified <tt>Format</tt>, unspecified attributes are
     * ignored.
     * <p>
     * The two <tt>Format</tt> objects do not have to be of the same class to
     * match. For example, if "A" are "B" are being compared, a match is
     * possible if "A" is derived from "B" or "B" is derived from "A". (The
     * compared attributes must still match, or <tt>matches</tt> fails.)
     * 
     * @param format
     *            The <tt>Format</tt> to compare with this one.
     * @return <tt>true</tt> if the specified <tt>Format</tt> matches this one,
     *         <tt>false</tt> if it does not.
     */
    @Override
    public boolean matches(Format format)
    {
        if (!super.matches(format))
            return false;
        if (!(format instanceof H264Format))
            return true;

        H264Format vf = (H264Format) format;

        return (size == null || vf.size == null || size.equals(vf.size))
                && (frameRate == NOT_SPECIFIED || vf.frameRate == NOT_SPECIFIED || frameRate == vf.frameRate);
    }

    /**
     * Generate a format that's less restrictive than this format but contains
     * the basic attributes that will make this resulting format useful for
     * format matching.
     * 
     * @return A <tt>Format</tt> that's less restrictive than the this format.
     */
    @Override
    public Format relax()
    {
        H264Format fmt;
        if ((fmt = (H264Format) super.relax()) == null)
            return null;

        fmt.size = null;
        fmt.maxDataLength = NOT_SPECIFIED;
        fmt.frameRate = NOT_SPECIFIED;

        return fmt;
    }

    /**
     * Gets a <tt>String</tt> representation of the attributes of this
     * <tt>VideoFormat</tt>. For example: "RGB, 352x240, ...".
     * 
     * @return A <tt>String</tt> that describes the <tt>VideoFormat</tt>
     *         attributes.
     */
    @Override
    public String toString()
    {
        String s = "";
        if (getEncoding() != null)
            s += getEncoding().toUpperCase();
        else
            s += "N/A";
        if (size != null)
            s += ", " + size.width + "x" + size.height;
        if (frameRate != NOT_SPECIFIED)
            s += ", FrameRate=" + ((int) (frameRate * 10) / 10f);
        if (maxDataLength != NOT_SPECIFIED)
            s += ", Length=" + maxDataLength;
        if (dataType != null && dataType != Format.byteArray)
            s += ", " + dataType;
        return s;
    }
}

