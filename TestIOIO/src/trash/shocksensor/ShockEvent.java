package trash.shocksensor;

import java.util.EventObject;


/**
* 衝突イベント情報を持つオブジェクト.
*
* @author TOYOTA, Yoichi
*
*/
public class ShockEvent extends EventObject {

    /**
* シリアルバージョンID.
*/
    private static final long serialVersionUID = -7174934985956233971L;

    /**
* 衝突判定開始時間.
*/
    private long startTime;

    /**
* 衝突時の加速度スカラ値の最大値.
*/
    private float maxAccelScala;

    /**
* イベントを作成する.
* @param source イベント発生元
*/
    public ShockEvent(final Object source) {
        super(source);
    }

    /**
* 衝突判定開始時間を記録する.
*
* @param nanoTime 衝突判定開始時間(ナノ秒)
*/
    public final void setStartTime(final long nanoTime) {
        this.startTime = nanoTime;
    }

    /**
* 衝突判定が開始してから、強制的にFlatに状態を戻す時間が経過したか.
*
* @param nanoTime 現在の時間
* @param checkPlusTimeout 加速度が減少に転じるまでのタイムアウトを計測するかどうか
* @return タイムアウト時にtrueを返す
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
* 衝突時の加速度スカラ値の最大値を設定する.
*
* @param scala 衝突時の加速度スカラ値の最大値
*/
    public final void setMaxAccelScala(final float scala) {
        this.maxAccelScala = scala;
    }

    /**
* 衝突時の加速度スカラ値の最大値を取得する.
*
* @return 衝突時の加速度スカラ値の最大値
*/
    public final float getMaxAccelScala() {
        return this.maxAccelScala;
    }

}