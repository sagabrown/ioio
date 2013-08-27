package ioio.robot.sensor.shocksensor;

/**
* 衝撃を感知した場合に通知されるイベントのリスナー.
*
* @author TOYOTA, Yoichi
*
*/
public interface ShockSensorListener {

    /**
* 衝撃を感知した場合に呼び出されるメソッド.
*
* @param event イベントオブジェクト
*/
    void onShocked(ShockEvent event);
}