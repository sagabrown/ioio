package ioio.robot.region.crawl.sensor.filter;

/**
* 簡易的なローパスフィルタを実現するクラス.
*
* vn = vn-1 * factor + input * (1-factor)
*
* vn: フィルタ通過後の値 vn-1: 前回のフィルタ通過後の値 factor: コンストラクタで指定するローパスフィルタの強さの値(0 < factor
* < 1) input: 入力値
*
* @author TOYOTA, Yoichi
*
*/
public class LowPassFilter implements Filter {

    /**
* フィルタの強度を表す係数.
*/
    private double factor;

    /**
* ローパスフィルタ通過後の値.
*/
    private double low;

    /**
* ローパスフィルタのインスタンスを生成する.
*
* @param pFactor フィルタ強度 (0 < factor < 1)
*/
    public LowPassFilter(final double pFactor) {
        this.factor = pFactor;
        this.low = Double.MAX_VALUE;
    }

    /**
* 入力値に対してローパスフィルタ処理を行う.
*
* @param input 入力値
* @return ローパスフィルタ通過後の出力値
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