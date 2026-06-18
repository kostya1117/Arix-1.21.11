package com.mojang.blaze3d.audio;

import java.nio.ByteBuffer;
import java.util.OptionalInt;
import javax.sound.sampled.AudioFormat;

import com.viaversion.viaaprilfools.api.AprilFoolsProtocolVersion;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.openal.AL10;


public class SoundBuffer {
    private @Nullable ByteBuffer data;
    private final AudioFormat format;
    private boolean hasAlBuffer;
    private int alBuffer;

    public SoundBuffer(ByteBuffer p_83798_, AudioFormat p_83799_) {
        this.data = p_83798_;
        this.format = p_83799_;
        if (ProtocolTranslator.getTargetVersion().equals(AprilFoolsProtocolVersion.s3d_shareware)) {
            this.viaFabricPlus$apply8BitSound(p_83798_);
        }
    }

    OptionalInt getAlBuffer() {
        if (!this.hasAlBuffer) {
            if (this.data == null) {
                return OptionalInt.empty();
            }

            int i = OpenAlUtil.audioFormatToOpenAl(this.format);
            int[] aint = new int[1];
            AL10.alGenBuffers(aint);
            if (OpenAlUtil.checkALError("Creating buffer")) {
                return OptionalInt.empty();
            }

            AL10.alBufferData(aint[0], i, this.data, (int)this.format.getSampleRate());
            if (OpenAlUtil.checkALError("Assigning buffer data")) {
                return OptionalInt.empty();
            }

            this.alBuffer = aint[0];
            this.hasAlBuffer = true;
            this.data = null;
        }

        return OptionalInt.of(this.alBuffer);
    }
    private void viaFabricPlus$apply8BitSound(final ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return;
        }
        if (this.format.getChannels() == 1) {
            this.viaFabricPlus$apply8BitMono(byteBuffer);
        } else {
            this.viaFabricPlus$apply8BitStereo(byteBuffer);
        }
    }

    private void viaFabricPlus$apply8BitMono(final ByteBuffer byteBuffer) {
        short short2 = 0;
        int integer3 = 0;
        while (byteBuffer.hasRemaining()) {
            if (integer3 == 0) {
                byteBuffer.mark();
                short2 = (short) (byteBuffer.getShort() & 0xFFFFFFFC);
                byteBuffer.reset();
                integer3 = 15;
            } else {
                --integer3;
            }
            byteBuffer.putShort(short2);
        }
        byteBuffer.flip();
    }

    private void viaFabricPlus$apply8BitStereo(final ByteBuffer byteBuffer) {
        short short2 = 0;
        short short3 = 0;
        int integer4 = 0;
        while (byteBuffer.hasRemaining()) {
            if (integer4 == 0) {
                byteBuffer.mark();
                short2 = (short) (byteBuffer.getShort() & 0xFFFFFFFC);
                short3 = (short) (byteBuffer.getShort() & 0xFFFFFFFC);
                byteBuffer.reset();
                integer4 = 15;
            } else {
                --integer4;
            }
            byteBuffer.putShort(short2);
            byteBuffer.putShort(short3);
        }
        byteBuffer.flip();
    }


    public void discardAlBuffer() {
        if (this.hasAlBuffer) {
            AL10.alDeleteBuffers(new int[]{this.alBuffer});
            if (OpenAlUtil.checkALError("Deleting stream buffers")) {
                return;
            }
        }

        this.hasAlBuffer = false;
    }

    public OptionalInt releaseAlBuffer() {
        OptionalInt optionalint = this.getAlBuffer();
        this.hasAlBuffer = false;
        return optionalint;
    }
}
