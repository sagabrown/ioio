package trash.shocksensor;

import ioio.robot.region.crawl.sensor.filter.Filter;
import ioio.robot.region.crawl.sensor.filter.LowPassFilter;

import java.util.ArrayList;
import java.util.List;


/**
* Android�̉����x�Z���T�[����Փˌn�̃A�N�V���������m���A�C�x���g��ʒm����N���X�B
*
* �d�͂���菜���������x�x�N�g���̑傫�����u�ԓI�ɕω�������Փ˂Ɣ��肷��B
* �����x�x�N�g���̑傫���̕ω��ʂ����A���l�̃v���X�̕ω��ʂ̌�A���l�̃}�C�i�X�̕ω��ʂ��m�F�ł�����Փ˂Ɣ��肷��
*
* �Փ˂̏ꍇ�A�u�ԓI�ɓ������~�܂邽�߁A��u�����傫�ȉ����x���[���̐i�s�����Ƌt�����ɔ�������B
* ���̉����x�̔������Ԃ̒����ɂ���āA�Փ˂ɂ���Ĕ����������̂��ǂ��������m����������Ƃ�B
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
* // �Փˎ��̃C�x���g�������L�q
* }
* }
* </pre>
*
* @author TOYOTA, Yoichi
*
*/
public class ShockSensor {

    /**
* �n�C�p�X�t�B���^�ɓn���t�B���^���x�̌W��.
*/
    public static final double FACTOR = 0.1;

    /**
* �����x�Z���T�[�̎���.
*/
    public static final int AXIS_COUNT = 3;

    /**
* �Փ˂����������ƌ��Ȃ��A�Փ˂̉����x�̕ω��ʂ�臒l.
*/
    public static final float ACCEL_DELTA_THRESHOLD = 200.0f;

    /**
* �Փ˔���J�n����A�����x�̏㏸���~�܂�܂ł̍ő厞��(�i�m�b).
* �����x�̕ω����v���X���}�C�i�X�̏�ԕω���H��ہA�����Őݒ肳�ꂽ���Ԉȓ��ɍs����K�v������B
*/
   public static final long SHOCK_EVENT_PLUS_DURATION = 50000000;

    /**
* �Փ˔���J�n����A���ۂɏՓ˃C�x���g�𔭐�������܂ł̍ő厞��(�i�m�b).
* �����x�̕ω����v���X���}�C�i�X���Î~�̏�ԕω���H��ہA�����Őݒ肳�ꂽ���Ԉȓ��ɍs����K�v������B
*/
    public static final long SHOCK_EVENT_DURATION = 200000000;

    /**
* �Փ˂Ɣ��肷�邽�߂̉����x�X�J���l��臒l.
*�@��Ԃ�AccelPlus����AccelMinus�ɕω������^�C�~���O�̉����x�X�J���l�����̒l�𒴂��Ă��Ȃ��ƏՓ˂Ɣ��肵�Ȃ��B
*/
    //public static final float ACCEL_THRESHOLD = 10.0f;
    public static final float ACCEL_THRESHOLD = 20.0f;

    /**
* �Փ˔��肪�I�������ƌ��Ȃ������x�X�J���l�̕ω��ʂ�臒l.
*/
    public static final float ACCEL_DELTA_LOW_THRESHOLD = 30.0f;

    /**
* 1�b������̃i�m�b.
*/
    public static final long NANO_SEC = 1000000000;

    /**
* �����Ŏw�肵���l���Z���Ԋu�̓��͖͂�������.
*/
    //public static final long IGNORE_DELTA_TIME_THRESHOLD = 100000;
    public static final long IGNORE_DELTA_TIME_THRESHOLD = 7500000;

    /**
* �Փ˃C�x���g�����������Ƃ��ɒʒm����C�x���g���X�i�[.
*/
    private List<ShockSensorListener> listeners =
            new ArrayList<ShockSensorListener>();

    /**
* �����x�Z���T�[�̓��͂���d�͂̉e������菜�����߂̃n�C�p�X�t�B���^.
*/
    private LinearAccelFilter filter = new LinearAccelFilter(FACTOR);

    /**
* �O��̓��͒l�ɂ������x�X�J���l.
*/
    private float prevScala = Float.MIN_VALUE;

    /**
* �O��̓��͂��s��ꂽ����(�i�m�b).
*/
    private long prevNanoTime = Long.MIN_VALUE;

    /**
* �Փ˃C�x���g�������̃C�x���g�I�u�W�F�N�g.
* �Z���T�[�̏�Ԃ�Flat����AccelPlus�ɕω������Ƃ��ɐ�������A�����x�̏����L�^����B
* ���ۂɏՓ˂Ɣ��肳�ꂽ��A�C�x���g���X�i�[�ɓn���B
* ��Ԃ�Flat�ɖ߂����ۂ�null����������B
*/
    private ShockEvent event;

    /**
* �Փ˃Z���T�[�̏�Ԃ�\���񋓎q.
*
* �Փ˂���������ƁA�u�ԓI�ɉ����x���[���̐Î~�̂��߂ɏオ��A��Ԃ�Flat��AccelPlus�ɕω�����B
* �����x�̏㏸�͏u�ԓI�ɏI��邽�߁A�����x�̕ω��͑����Ƀ}�C�i�X�ɓ]����B
* �����AccelPlus�̏�ԂŊ��m�����AccelPlus��AccelMinus�ɕω�����B
* �����x�̕ω��͂����Ɏ��܂邽�߁A�����x�̕ω���AccelMinus�̏�Ԃ�0�ɋ߂��Ȃ�����Flat�ɏ�Ԃ��߂�A
* �Փ˃C�x���g�𔭐�������
*
* @author TOYOTA Yoichi
*
*/
    public enum SensorStatus {
        /**
* �Ռ��Ɋւ���C�x���g���������Ă��Ȃ����.
*/
        Flat,
        /**
* 臒l�𒴂����v���X�̉����x�̕ω����󂯎��A�}�C�i�X�̉����x�̊Ď����s���Ă�����.
*/
        AccelPlus,
        /**
* 臒l�𒴂����}�C�i�X�̉����x�̕ω����󂯎��A�����x�̕ω���0�Ȃ�Ď����s���Ă�����.
*/
        AccelMinus
    };

    /**
* �Փ˃Z���T�[�̌��݂̏��.
*/
    private SensorStatus status = SensorStatus.Flat;

    /**
* �����x�Z���T�[����̓��͂��󂯎��A�Փ˂�����������o�^����Ă��郊�X�i�[�ɏՓ˔����̃C�x���g��ʒm����.
*
* @param accel
* �����x�Z���T�[����擾����l (accel[0]: x�������x�A accel[1]: y�������x, accel[2]:
* z�������x)
* @param nanoTime �����x�Z���T�[������͂����������� (�i�m�b)
*/
    public final void input(final float[] accel, final long nanoTime) {
        // ���͒l��HPF�����āA�d�͂̉e������菜��
        float[] linearAccel = filter.filter(accel);

        // �����x�̃X�J���l�����߂�
        float scala = getScala(linearAccel);

        // �ŏ��̓��͂̏ꍇ�A���������Ȃ��̂Œl�̋L�^�݂̂��s���I������
        if (this.prevScala == Float.MIN_VALUE) {
            this.prevScala = scala;
            this.prevNanoTime = nanoTime;
            return;
        }

        // �O����͂���ɒ[�Ɏ��Ԃ��Z���ꍇ�A�����x�����̌덷���傫���Ȃ邽�߁A����𖳎�����
        if (nanoTime - this.prevNanoTime < IGNORE_DELTA_TIME_THRESHOLD) {
            return;
        }

        // �����x�̃X�J���l�̕ω��ʂ����߂�(�i�m�b�P��)
        float deltaScala =
                (scala - this.prevScala)
                / ((float) (nanoTime - this.prevNanoTime) / NANO_SEC);

        // �X�J���l�̕ω��ʂ���莞�ԓ��Ƀv���X���}�C�i�X��0�ɂȂ�ΏՓ˂����������ƌ��Ȃ�
        // �ω��ʂ̃v���X��臒l�����߁A����𒴂�����ω����Ď�����
        // �Ƃ肠����0.1sec���ɏ�L�̕ω�������������C�x���g�𔭉΂���
        switch (this.status) {
        case Flat:
            if (deltaScala > ACCEL_DELTA_THRESHOLD) {
                this.status = SensorStatus.AccelPlus;
                this.event = new ShockEvent(this);
                this.event.setStartTime(nanoTime);
            }
            break;
        case AccelPlus:
            // AccelPlus�ɏ�Ԃ��ω����Ă����莞�Ԃ��o�߂�����Flat�ɏ�Ԃ��߂�
            if (this.event.isTimeout(nanoTime, true)) {
                this.status = SensorStatus.Flat;
                this.event = null;
                break;
            }
            // �X�J���l�̕ω��ʂ��}�C�i�X�ɂȂ����ہA���̎��_�ł̉����x�X�J�������l�𒴂��Ă���Ώ�Ԃ�AccelMinus��
            if (deltaScala < 0) {
                if (this.prevScala < ACCEL_THRESHOLD) {
                    // �����x�̃X�J���l�����l�𒴂��Ă��Ȃ����Flat�ɏ�Ԃ��߂�
                    this.status = SensorStatus.Flat;
                    this.event = null;
                    break;
                }
                // �����x�̃X�J���l�����l�𒴂��Ă���Ώ�Ԃ�AccelMinus��
                this.status = SensorStatus.AccelMinus;
                this.event.setMaxAccelScala(this.prevScala);
            }
            break;
        case AccelMinus:
            // AccelPlus�ɏ�Ԃ��ω����Ă����莞�Ԃ��o�߂�����Flat�ɏ�Ԃ��߂�
            if (this.event.isTimeout(nanoTime, false)) {
                this.status = SensorStatus.Flat;
                this.event = null;
                break;
            }
            // �����x�̃X�J���l�̐�Βl�����l�������Ώ�Ԃ�Flat�ɖ߂�����ŁA�C�x���g����
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
* �Փ˃C�x���g�����X�i�[�ɒʒm����.
*
* @param shockEvent �Փ˃C�x���g�I�u�W�F�N�g
*/
    private void fireShockEvent(final ShockEvent shockEvent) {
        for (ShockSensorListener listener : this.listeners) {
            listener.onShocked(shockEvent);
        }
    }

    /**
* �Փ˃C�x���g�����������Ƃ��ɒʒm���郊�X�i�[��ǉ�����.
*
* @param listener �ǉ�����C�x���g���X�i�[
*/
    public final void addShockSensorListener(
            final ShockSensorListener listener) {
        this.listeners.add(listener);
    }

    /**
* �Փ˃C�x���g�����������Ƃ��ɒʒm���郊�X�i�[���폜����.
*
* @param listener �폜����C�x���g���X�i�[
*/
    public final void removeShockSensorListener(
            final ShockSensorListener listener) {
        this.listeners.remove(listener);
    }

    /**
* �x�N�g���̃X�J���l���擾���� �e�v�f��2��̘a�̕�������Ԃ������̊֐�.
*
* @param vector �X�J���l�����߂����x�N�g��
* @return �X�J���l
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
* 3���̉����x����d�͉����x����菜�����߂̃t�B���^�N���X.
*
* @author TOYOTA, Yoichi
*
*/
class LinearAccelFilter {
    /**
* X���AY���AZ���p�̃��[�p�X�t�B���^.
*/
    private Filter[] lowPassFilters;

    /**
* �����x�Z���T�[���瓾����l����d�͉����x����菜���N���X���쐬����.
*
* @param factor ���[�p�X�t�B���^�ɓn���W��
*/
    public LinearAccelFilter(final double factor) {
        // X���AY���AZ���p�̃t�B���^��p�ӂ���
        lowPassFilters = new Filter[ShockSensor.AXIS_COUNT];
        for (int i = 0; i < lowPassFilters.length; i++) {
            lowPassFilters[i] = new LowPassFilter(factor);
        }
    }

    /**
* 3�������x����n�C�p�X�t�B���^�𗘗p���āA�d�͉����x����菜�������ʂ�Ԃ�.
*
* @param input
* 3�������x�Z���T�[���瓾��������x
* @return �d�͉����x����菜�������`�����x
*/
    public float[] filter(final float[] input) {
        float[] retval = new float[ShockSensor.AXIS_COUNT];
        for (int i = 0; i < retval.length; i++) {
            // ���͒l���烍�[�p�X�t�B���^�̌��ʂ��������ƂŁA�n�C�p�X�t�B���^������
            double filtered = lowPassFilters[i].filter(input[i]);
            retval[i] = (float) (input[i] - filtered);
        }

        return retval;
    }
}
