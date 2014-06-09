package trash.shocksensor;

import java.util.EventObject;


/**
* �Փ˃C�x���g�������I�u�W�F�N�g.
*
* @author TOYOTA, Yoichi
*
*/
public class ShockEvent extends EventObject {

    /**
* �V���A���o�[�W����ID.
*/
    private static final long serialVersionUID = -7174934985956233971L;

    /**
* �Փ˔���J�n����.
*/
    private long startTime;

    /**
* �Փˎ��̉����x�X�J���l�̍ő�l.
*/
    private float maxAccelScala;

    /**
* �C�x���g���쐬����.
* @param source �C�x���g������
*/
    public ShockEvent(final Object source) {
        super(source);
    }

    /**
* �Փ˔���J�n���Ԃ��L�^����.
*
* @param nanoTime �Փ˔���J�n����(�i�m�b)
*/
    public final void setStartTime(final long nanoTime) {
        this.startTime = nanoTime;
    }

    /**
* �Փ˔��肪�J�n���Ă���A�����I��Flat�ɏ�Ԃ�߂����Ԃ��o�߂�����.
*
* @param nanoTime ���݂̎���
* @param checkPlusTimeout �����x�������ɓ]����܂ł̃^�C���A�E�g���v�����邩�ǂ���
* @return �^�C���A�E�g����true��Ԃ�
*/
    public final boolean isTimeout(final long nanoTime,
            final boolean checkPlusTimeout) {
        if (checkPlusTimeout) {
            if (nanoTime - this.startTime
                    > ShockSensor.SHOCK_EVENT_PLUS_DURATION) {
                return true;
            }
        }
        return (nanoTime - this.startTime > ShockSensor.SHOCK_EVENT_DURATION);
    }

    /**
* �Փˎ��̉����x�X�J���l�̍ő�l��ݒ肷��.
*
* @param scala �Փˎ��̉����x�X�J���l�̍ő�l
*/
    public final void setMaxAccelScala(final float scala) {
        this.maxAccelScala = scala;
    }

    /**
* �Փˎ��̉����x�X�J���l�̍ő�l���擾����.
*
* @return �Փˎ��̉����x�X�J���l�̍ő�l
*/
    public final float getMaxAccelScala() {
        return this.maxAccelScala;
    }

}