package ioio.robot.region.crawl.sensor.filter;

/**
* �ȈՓI�ȃ��[�p�X�t�B���^����������N���X.
*
* vn = vn-1 * factor + input * (1-factor)
*
* vn: �t�B���^�ʉߌ�̒l vn-1: �O��̃t�B���^�ʉߌ�̒l factor: �R���X�g���N�^�Ŏw�肷�郍�[�p�X�t�B���^�̋����̒l(0 < factor
* < 1) input: ���͒l
*
* @author TOYOTA, Yoichi
*
*/
public class LowPassFilter implements Filter {

    /**
* �t�B���^�̋��x��\���W��.
*/
    private double factor;

    /**
* ���[�p�X�t�B���^�ʉߌ�̒l.
*/
    private double low;

    /**
* ���[�p�X�t�B���^�̃C���X�^���X�𐶐�����.
*
* @param pFactor �t�B���^���x (0 < factor < 1)
*/
    public LowPassFilter(final double pFactor) {
        this.factor = pFactor;
        this.low = Double.MAX_VALUE;
    }

    /**
* ���͒l�ɑ΂��ă��[�p�X�t�B���^�������s��.
*
* @param input ���͒l
* @return ���[�p�X�t�B���^�ʉߌ�̏o�͒l
*/
    public final double filter(final double input) {
        if (low == Double.MAX_VALUE) {
            low = input;
            return low;
        }

        low = input * factor + low * (1 - factor);
        return low;
    }

}