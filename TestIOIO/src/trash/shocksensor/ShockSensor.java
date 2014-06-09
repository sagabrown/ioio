package trash.shocksensor;

import ioio.robot.region.crawl.sensor.filter.Filter;
import ioio.robot.region.crawl.sensor.filter.LowPassFilter;

import java.util.ArrayList;
import java.util.List;


/**
* Androidの加速度センサーから衝突系のアクションを感知し、イベントを通知するクラス。
*
* 重力を取り除いた加速度ベクトルの大きさが瞬間的に変化したら衝突と判定する。
* 加速度ベクトルの大きさの変化量を取り、一定値のプラスの変化量の後、一定値のマイナスの変化量が確認できたら衝突と判定する
*
* 衝突の場合、瞬間的に動きが止まるため、一瞬だけ大きな加速度が端末の進行方向と逆向きに発生する。
* その加速度の発生時間の長さによって、衝突によって発生したものかどうかを検知する方式をとる。
*
* <pre>
* import jp.co.xtone.android.shocksensor.*;
* ...
*
* public class TestActivity extends Activity
* implements ShockSensorListener, SensorEventListener {
* private ShockSensor shockSensor = new ShockSensor();
*
* public void onCreate(Bundle savedInstanceState) {
* super.onCreate(savedInstanceState);
* SensorManager manager =
* (SensorManager)getSystemService(SENSOR_SERVICE);
* List<Sensor> sensors =
* manager.getSensorList(Sensor.TYPE_ACCELEROMETER);
* if (sensors.size() > 0) {
* Sensor s = sensors.get(0);
* manager.registerListener(this, s);
* }
* shockSensor.addShockSensorListener(this);
* }
* ....
* public void onSensorChanged(SensorEvent event) {
* shockSensor.input(event.values);
* }
*
* public void onShocked(ShockSensorEvent event) {
* // 衝突時のイベント処理を記述
* }
* }
* </pre>
*
* @author TOYOTA, Yoichi
*
*/
public class ShockSensor {

    /**
* ハイパスフィルタに渡すフィルタ強度の係数.
*/
    public static final double FACTOR = 0.1;

    /**
* 加速度センサーの軸数.
*/
    public static final int AXIS_COUNT = 3;

    /**
* 衝突が発生したと見なす、衝突の加速度の変化量の閾値.
*/
    public static final float ACCEL_DELTA_THRESHOLD = 200.0f;

    /**
* 衝突判定開始から、加速度の上昇が止まるまでの最大時間(ナノ秒).
* 加速度の変化がプラス→マイナスの状態変化を辿る際、ここで設定された時間以内に行われる必要がある。
*/
   public static final long SHOCK_EVENT_PLUS_DURATION = 50000000;

    /**
* 衝突判定開始から、実際に衝突イベントを発生させるまでの最大時間(ナノ秒).
* 加速度の変化がプラス→マイナス→静止の状態変化を辿る際、ここで設定された時間以内に行われる必要がある。
*/
    public static final long SHOCK_EVENT_DURATION = 200000000;

    /**
* 衝突と判定するための加速度スカラ値の閾値.
*　状態がAccelPlusからAccelMinusに変化したタイミングの加速度スカラ値がこの値を超えていないと衝突と判定しない。
*/
    //public static final float ACCEL_THRESHOLD = 10.0f;
    public static final float ACCEL_THRESHOLD = 20.0f;

    /**
* 衝突判定が終了したと見なす加速度スカラ値の変化量の閾値.
*/
    public static final float ACCEL_DELTA_LOW_THRESHOLD = 30.0f;

    /**
* 1秒あたりのナノ秒.
*/
    public static final long NANO_SEC = 1000000000;

    /**
* ここで指定した値より短い間隔の入力は無視する.
*/
    //public static final long IGNORE_DELTA_TIME_THRESHOLD = 100000;
    public static final long IGNORE_DELTA_TIME_THRESHOLD = 7500000;

    /**
* 衝突イベントが発生したときに通知するイベントリスナー.
*/
    private List<ShockSensorListener> listeners =
            new ArrayList<ShockSensorListener>();

    /**
* 加速度センサーの入力から重力の影響を取り除くためのハイパスフィルタ.
*/
    private LinearAccelFilter filter = new LinearAccelFilter(FACTOR);

    /**
* 前回の入力値による加速度スカラ値.
*/
    private float prevScala = Float.MIN_VALUE;

    /**
* 前回の入力が行われた時間(ナノ秒).
*/
    private long prevNanoTime = Long.MIN_VALUE;

    /**
* 衝突イベント発生時のイベントオブジェクト.
* センサーの状態がFlatからAccelPlusに変化したときに生成され、加速度の情報を記録する。
* 実際に衝突と判定されたら、イベントリスナーに渡す。
* 状態がFlatに戻った際にnullが代入される。
*/
    private ShockEvent event;

    /**
* 衝突センサーの状態を表す列挙子.
*
* 衝突が発生すると、瞬間的に加速度が端末の静止のために上がり、状態がFlat→AccelPlusに変化する。
* 加速度の上昇は瞬間的に終わるため、加速度の変化は即座にマイナスに転じる。
* これをAccelPlusの状態で感知するとAccelPlus→AccelMinusに変化する。
* 加速度の変化はすぐに収まるため、加速度の変化がAccelMinusの状態で0に近くなったらFlatに状態が戻り、
* 衝突イベントを発生させる
*
* @author TOYOTA Yoichi
*
*/
    public enum SensorStatus {
        /**
* 衝撃に関するイベントが発生していない状態.
*/
        Flat,
        /**
* 閾値を超えたプラスの加速度の変化を受け取り、マイナスの加速度の監視を行っている状態.
*/
        AccelPlus,
        /**
* 閾値を超えたマイナスの加速度の変化を受け取り、加速度の変化が0なる監視を行っている状態.
*/
        AccelMinus
    };

    /**
* 衝突センサーの現在の状態.
*/
    private SensorStatus status = SensorStatus.Flat;

    /**
* 加速度センサーからの入力を受け取り、衝突が発生したら登録されているリスナーに衝突発生のイベントを通知する.
*
* @param accel
* 加速度センサーから取得する値 (accel[0]: x軸加速度、 accel[1]: y軸加速度, accel[2]:
* z軸加速度)
* @param nanoTime 加速度センサーから入力があった時間 (ナノ秒)
*/
    public final void input(final float[] accel, final long nanoTime) {
        // 入力値にHPFかけて、重力の影響を取り除く
        float[] linearAccel = filter.filter(accel);

        // 加速度のスカラ値を求める
        float scala = getScala(linearAccel);

        // 最初の入力の場合、差分が取れないので値の記録のみを行い終了する
        if (this.prevScala == Float.MIN_VALUE) {
            this.prevScala = scala;
            this.prevNanoTime = nanoTime;
            return;
        }

        // 前回入力から極端に時間が短い場合、加速度差分の誤差が大きくなるため、判定を無視する
        if (nanoTime - this.prevNanoTime < IGNORE_DELTA_TIME_THRESHOLD) {
            return;
        }

        // 加速度のスカラ値の変化量を求める(ナノ秒単位)
        float deltaScala =
                (scala - this.prevScala)
                / ((float) (nanoTime - this.prevNanoTime) / NANO_SEC);

        // スカラ値の変化量が一定時間内にプラス→マイナス→0になれば衝突が発生したと見なす
        // 変化量のプラスの閾値を決め、それを超えたら変化を監視する
        // とりあえず0.1sec内に上記の変化が発生したらイベントを発火する
        switch (this.status) {
        case Flat:
            if (deltaScala > ACCEL_DELTA_THRESHOLD) {
                this.status = SensorStatus.AccelPlus;
                this.event = new ShockEvent(this);
                this.event.setStartTime(nanoTime);
            }
            break;
        case AccelPlus:
            // AccelPlusに状態が変化してから一定時間が経過したらFlatに状態が戻る
            if (this.event.isTimeout(nanoTime, true)) {
                this.status = SensorStatus.Flat;
                this.event = null;
                break;
            }
            // スカラ値の変化量がマイナスになった際、その時点での加速度スカラが一定値を超えていれば状態をAccelMinusに
            if (deltaScala < 0) {
                if (this.prevScala < ACCEL_THRESHOLD) {
                    // 加速度のスカラ値が一定値を超えていなければFlatに状態が戻る
                    this.status = SensorStatus.Flat;
                    this.event = null;
                    break;
                }
                // 加速度のスカラ値が一定値を超えていれば状態をAccelMinusに
                this.status = SensorStatus.AccelMinus;
                this.event.setMaxAccelScala(this.prevScala);
            }
            break;
        case AccelMinus:
            // AccelPlusに状態が変化してから一定時間が経過したらFlatに状態が戻る
            if (this.event.isTimeout(nanoTime, false)) {
                this.status = SensorStatus.Flat;
                this.event = null;
                break;
            }
            // 加速度のスカラ値の絶対値が一定値を下回れば状態をFlatに戻した上で、イベント発火
            if (Math.abs(deltaScala) < ACCEL_DELTA_LOW_THRESHOLD) {
                this.status = SensorStatus.Flat;
                this.fireShockEvent(this.event);
            }
            break;
        default:
            break;
        }

        this.prevScala = scala;
        this.prevNanoTime = nanoTime;
    }

    /**
* 衝突イベントをリスナーに通知する.
*
* @param shockEvent 衝突イベントオブジェクト
*/
    private void fireShockEvent(final ShockEvent shockEvent) {
        for (ShockSensorListener listener : this.listeners) {
            listener.onShocked(shockEvent);
        }
    }

    /**
* 衝突イベントが発生したときに通知するリスナーを追加する.
*
* @param listener 追加するイベントリスナー
*/
    public final void addShockSensorListener(
            final ShockSensorListener listener) {
        this.listeners.add(listener);
    }

    /**
* 衝突イベントが発生したときに通知するリスナーを削除する.
*
* @param listener 削除するイベントリスナー
*/
    public final void removeShockSensorListener(
            final ShockSensorListener listener) {
        this.listeners.remove(listener);
    }

    /**
* ベクトルのスカラ値を取得する 各要素の2乗の和の平方根を返すだけの関数.
*
* @param vector スカラ値を求めたいベクトル
* @return スカラ値
*/
    private float getScala(final float[] vector) {
        float scala = 0;
        for (int i = 0; i < vector.length; i++) {
            scala += Math.pow(vector[i], 2);
        }
        return (float) Math.sqrt(scala);
    }
}

/**
* 3軸の加速度から重力加速度を取り除くためのフィルタクラス.
*
* @author TOYOTA, Yoichi
*
*/
class LinearAccelFilter {
    /**
* X軸、Y軸、Z軸用のローパスフィルタ.
*/
    private Filter[] lowPassFilters;

    /**
* 加速度センサーから得られる値から重力加速度を取り除くクラスを作成する.
*
* @param factor ローパスフィルタに渡す係数
*/
    public LinearAccelFilter(final double factor) {
        // X軸、Y軸、Z軸用のフィルタを用意する
        lowPassFilters = new Filter[ShockSensor.AXIS_COUNT];
        for (int i = 0; i < lowPassFilters.length; i++) {
            lowPassFilters[i] = new LowPassFilter(factor);
        }
    }

    /**
* 3軸加速度からハイパスフィルタを利用して、重力加速度を取り除いた結果を返す.
*
* @param input
* 3軸加速度センサーから得られる加速度
* @return 重力加速度を取り除いた線形加速度
*/
    public float[] filter(final float[] input) {
        float[] retval = new float[ShockSensor.AXIS_COUNT];
        for (int i = 0; i < retval.length; i++) {
            // 入力値からローパスフィルタの結果を引くことで、ハイパスフィルタを実現
            double filtered = lowPassFilters[i].filter(input[i]);
            retval[i] = (float) (input[i] - filtered);
        }

        return retval;
    }
}
