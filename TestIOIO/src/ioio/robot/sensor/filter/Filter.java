package ioio.robot.sensor.filter;

/**
* ローパスフィルタ等、実数を入力し、実数を出力として得られるフィルタのためのインターフェイス.
*
* @author TOYOTA, Yoichi
*
*/
public interface Filter {

    /**
* フィルタ処理を行う.
*
* @param input 入力値
* @return フィルターされた出力値
*/
    double filter(double input);
}