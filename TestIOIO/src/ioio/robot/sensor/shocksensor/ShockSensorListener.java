package ioio.robot.sensor.shocksensor;

/**
* �Ռ������m�����ꍇ�ɒʒm�����C�x���g�̃��X�i�[.
*
* @author TOYOTA, Yoichi
*
*/
public interface ShockSensorListener {

    /**
* �Ռ������m�����ꍇ�ɌĂяo����郁�\�b�h.
*
* @param event �C�x���g�I�u�W�F�N�g
*/
    void onShocked(ShockEvent event);
}