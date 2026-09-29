/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.udp.session;

public class Frame {
    private final short frameId;
    private final byte[][] segments;
    private final boolean[] acks;
    private final long timeCreated;
    private long timeLastTransmitted;

    public Frame(short frameId, short totalSegments) {
        this.frameId = frameId;
        this.segments = new byte[totalSegments][];
        this.acks = new boolean[totalSegments];
        this.timeCreated = System.currentTimeMillis();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void setSegment(short segmentId, byte[] segment, boolean ack) {
        byte[][] byArray = this.segments;
        synchronized (this.segments) {
            this.segments[segmentId] = segment;
            this.acks[segmentId] = ack;
            // ** MonitorExit[var4_4] (shouldn't be in output)
            return;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void acknowledge(short segmentId) {
        byte[][] byArray = this.segments;
        synchronized (this.segments) {
            this.acks[segmentId] = true;
            // ** MonitorExit[var2_2] (shouldn't be in output)
            return;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean isAcknowledged(short segmentId) {
        byte[][] byArray = this.segments;
        synchronized (this.segments) {
            // ** MonitorExit[var2_2] (shouldn't be in output)
            return this.acks[segmentId];
        }
    }

    public short getFrameId() {
        return this.frameId;
    }

    public short getTotalSegments() {
        return (short)this.segments.length;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public short getAcknowledgeSegments() {
        byte[][] byArray = this.segments;
        synchronized (this.segments) {
            short ackedSegments = 0;
            for (boolean ack : this.acks) {
                if (!ack) continue;
                ackedSegments = (short)(ackedSegments + 1);
            }
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return ackedSegments;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public byte[] getData(short segmentId) {
        byte[][] byArray = this.segments;
        synchronized (this.segments) {
            // ** MonitorExit[var2_2] (shouldn't be in output)
            return this.segments[segmentId];
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean isComplete() {
        byte[][] byArray = this.segments;
        synchronized (this.segments) {
            for (boolean receivedSegment : this.acks) {
                if (receivedSegment) continue;
                // ** MonitorExit[var1_1] (shouldn't be in output)
                return false;
            }
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return true;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void markTransmit() {
        byte[][] byArray = this.segments;
        synchronized (this.segments) {
            this.timeLastTransmitted = System.currentTimeMillis();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean isSegmentLost(short segmentId) {
        byte[][] byArray = this.segments;
        synchronized (this.segments) {
            // ** MonitorExit[var2_2] (shouldn't be in output)
            return !this.isAcknowledged(segmentId) && System.currentTimeMillis() - this.timeLastTransmitted > 300L;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean isTimeout() {
        byte[][] byArray = this.segments;
        synchronized (this.segments) {
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return System.currentTimeMillis() - this.timeCreated > 5000L;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public byte headerByte() {
        if (this.segments.length == 0) {
            return 0;
        }
        byte[][] byArray = this.segments;
        synchronized (this.segments) {
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return this.segments[0] != null ? this.segments[0][0] : (byte)0;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public byte[] bytes() {
        byte[][] byArray = this.segments;
        synchronized (this.segments) {
            int length = 0;
            for (byte[] segment : this.segments) {
                if (segment == null || segment.length <= 0) continue;
                length += segment.length;
            }
            if (length == 0) {
                // ** MonitorExit[var1_1] (shouldn't be in output)
                return new byte[0];
            }
            byte[] bytes = new byte[length];
            int offset = 0;
            for (byte[] segment : this.segments) {
                if (segment == null || segment.length == 0) continue;
                System.arraycopy(segment, 0, bytes, offset, segment.length);
                offset += segment.length;
            }
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return bytes;
        }
    }

    public boolean isResetBuffer() {
        return this.getTotalSegments() == 1 && this.segments[0] != null && this.segments[0].length == 0;
    }
}
