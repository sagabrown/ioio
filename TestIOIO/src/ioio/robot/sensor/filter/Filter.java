package ioio.robot.sensor.filter;

/**
* ���[�p�X�t�B���^���A��������͂��A�������o�͂Ƃ��ē�����t�B���^�̂��߂̃C���^�[�t�F�C�X.
*
* @author TOYOTA, Yoichi
*
*/
public interface Filter {

    /**
* �t�B���^�������s��.
*
* @param input ���͒l
* @return �t�B���^�[���ꂽ�o�͒l
*/
    double filter(double input);
}