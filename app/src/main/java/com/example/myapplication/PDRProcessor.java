package com.example.myapplication;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.hardware.SensorManager;
import android.util.Log;

import android.util.Log;
import java.util.List;

public class PDRProcessor {
    private static final String TAG = "PDRData"; // 定义日志标记

    public static class HeadingData {
        public final double timestamp;
        public final float headingAngle;

        public HeadingData(double timestamp, float headingAngle) {
            this.timestamp = timestamp;
            this.headingAngle = headingAngle;
        }
    }


    public List<double[]> processSensorData(List<String> sensorDataLines) {
        // 检查列表是否为空
        if (sensorDataLines == null || sensorDataLines.isEmpty()) {
            Log.w(TAG, "没有读取到传感器数据。"); // 使用警告级别的日志
            return null;
        }

        // 创建列表以存储不同传感器的数据
        List<SensorData> accelerometerData = new ArrayList<>();
        List<SensorData> gyroscopeData = new ArrayList<>();
        List<SensorData> magnetometerData = new ArrayList<>();

        // 解析每一行数据并将其存储在相应的列表中
        for (String dataLine : sensorDataLines) {
            String[] dataParts = dataLine.trim().split("\\s+");
            if (dataParts.length < 5) {
                Log.w(TAG, "数据格式错误: " + dataLine);
                continue;
            }
            try {
                int sensorType = Integer.parseInt(dataParts[0]);
                double timestamp = Double.parseDouble(dataParts[1]);
                // 此处调整手机轴系，xyz分别对应前右下
                double x = Double.parseDouble(dataParts[3]);
                double y = Double.parseDouble(dataParts[2]);
                double z = -Double.parseDouble(dataParts[4]);

                SensorData sensorData = new SensorData(sensorType, timestamp, x, y, z);
                switch (sensorType) {
                    case 1: // 加速度计数据
                        accelerometerData.add(sensorData);
                        break;
                    case 2: // 陀螺仪数据
                        gyroscopeData.add(sensorData);
                        break;
                    case 3: // 磁力计数据
                        magnetometerData.add(sensorData);
                        break;
                    default:
                        Log.w(TAG, "未知的传感器类型: " + sensorType);
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "解析错误: " + dataLine);
            }
        }

        // 创建同步后数据的列表
        List<SensorData> syncedAccelerometerData = new ArrayList<>();
        List<SensorData> syncedGyroscopeData = new ArrayList<>();
        List<SensorData> syncedMagnetometerData = new ArrayList<>();

        // 调用时间同步方法
        synchronizeSensorData(accelerometerData, gyroscopeData, magnetometerData,
                syncedAccelerometerData, syncedGyroscopeData, syncedMagnetometerData);

        List<SensorData> syncedAccelerometerDatafiltered = filterSensorData(syncedAccelerometerData, 3);
        List<SensorData> syncedGyroscopeDatafiltered = filterSensorData(syncedGyroscopeData, 3);
        List<SensorData> syncedMagnetometerfiltered = filterSensorData(syncedMagnetometerData, 3);

        // 使用陀螺仪数据和加速度计数据计算航向角和时间戳
        List<HeadingData> headingDataList = calculateOrientationUsingGyroscope(syncedGyroscopeDatafiltered,syncedAccelerometerDatafiltered);

        // 调用步伐探测方法
        List<Double> stepTimestamps = detectSteps(syncedAccelerometerDatafiltered, 9.8);

        // 调用航迹推算方法
        List<double[]> trajectory = calculateTrajectory(stepTimestamps, headingDataList, syncedAccelerometerDatafiltered);


        // 打印轨迹点到日志
        for (double[] position : trajectory) {
            double posX = position[0];
            double posY = position[1];
            Log.d(TAG, "X: " + posX + ", Y: " + posY);
        }
        // 返回计算出的二维轨迹坐标
        return trajectory;

    }

    /***** 轨迹推算的函数 *****/
    public List<double[]> calculateTrajectory(List<Double> stepsTimestamps, List<HeadingData> eulerAnglesData, List<SensorData> syncedAccelerometerData) {
        double posX = 0;
        double posY = 0;
        List<double[]> positions = new ArrayList<>();
        positions.add(new double[]{posX, posY}); // 存储每一步的位置

        double lastStepTime = stepsTimestamps.get(0); // 初始化上一次脚步的时间为第一次脚步的时间

        for (double stepTime : stepsTimestamps) {
            // 计算时间差
            double dT = stepTime - lastStepTime;

            // 更新上一次步骤的时间
            lastStepTime = stepTime;

            // 找到对应的航向角
            double headingAngle = findHeadingAngle(eulerAnglesData, stepTime);

            // 步长估计
            double acc_norm=0;
            for(SensorData data : syncedAccelerometerData){
                if (data.timestamp<=stepTime){
                    acc_norm=Math.sqrt(data.x*data.x+data.y*data.y+data.z*data.z);
                }else{
                    break;
                }


            }
            double sf=(dT==0)?0:1/dT;
            double stepLength = 0.132*(acc_norm-9.8)+0.123*sf+0.225;;

            // 更新位置
            double[] newPos = updatePosition(posX, posY, stepLength, headingAngle);
            posX = newPos[0];
            posY = newPos[1];

            // 将新位置添加到数组
            positions.add(new double[]{posX, posY});

        }

        return positions;
    }

    // 辅助方法：根据步伐时间戳找到对应的航向角
    private double findHeadingAngle(List<HeadingData> eulerAnglesData, double stepTime) {
        double headingAngle = 0;
        for (HeadingData data : eulerAnglesData) {
            if (data.timestamp <= stepTime) {
                headingAngle = data.headingAngle;
            } else {
                break;
            }
        }
        return headingAngle;
    }

    // 辅助方法：根据当前位置、步长和航向角更新位置
    private double[] updatePosition(double currentPosX, double currentPosY, double stepLength, double headingAngleRad) {
        double newX = currentPosX + stepLength * Math.cos(headingAngleRad);
        double newY = currentPosY + stepLength * Math.sin(headingAngleRad);
        return new double[]{newX, newY};
    }


    /***** 获取脚步发生历元的函数 *****/
    // 添加步伐探测方法
    public List<Double> detectSteps(List<SensorData> syncedAccelerometerData, double threshold) {
        List<Double> stepsDetected = new ArrayList<>();
        double[] lastStepTime = {0, 0}; // 初始化为0

        // 计算加速度模长
        for (int i = 1; i < syncedAccelerometerData.size() - 1; i++) {
            double accelMagnitude = Math.sqrt(
                    Math.pow(syncedAccelerometerData.get(i).x, 2) +
                            Math.pow(syncedAccelerometerData.get(i).y, 2) +
                            Math.pow(syncedAccelerometerData.get(i).z, 2)
            );

            // 使用连续三个历元进行峰值探测
            if (accelMagnitude > Math.max(accelMagnitude(syncedAccelerometerData.get(i - 1)),
                    accelMagnitude(syncedAccelerometerData.get(i + 1))) &&
                    accelMagnitude > threshold) {
                double currentTime = syncedAccelerometerData.get(i).timestamp;
                if (currentTime - lastStepTime[0] > 1) { // 检查时间间隔是否大于1秒
                    stepsDetected.add(currentTime);
                    lastStepTime[0] = lastStepTime[1];
                    lastStepTime[1] = currentTime;
                }
            }
        }

        return stepsDetected;
    }

    // 辅助方法：计算加速度模长
    private double accelMagnitude(SensorData data) {
        return Math.sqrt(Math.pow(data.x, 2) + Math.pow(data.y, 2) + Math.pow(data.z, 2));
    }


    /***** 获取航向角函数 *****/
    // 使用陀螺仪数据计算航向角
    private List<HeadingData> calculateOrientationUsingGyroscope(List<SensorData> syncedGyroscopeData,List<SensorData> syncedAccelerometerData) {

        // 计算初始的水平姿态角
        float[] initialOrientation = calculateInitialOrientation(syncedAccelerometerData);
        float roll0 = initialOrientation[0];
        float pitch0 = initialOrientation[1];
        float yaw0 = 0; // 航向角初始化为0

        // 初始化四元数
        float[] quaternion = new float[]{1, 0, 0, 0};
        quaternion = eulerToQuaternion(yaw0, pitch0, roll0);

        List<HeadingData> headingDataList = new ArrayList<>();

        float[] eInt= new float[]{0,0,0};
        // 遍历每个陀螺仪数据点
        for (int i = 0; i < syncedGyroscopeData.size(); i++) {
            SensorData Gyrodata = syncedGyroscopeData.get(i);
            SensorData Accdata=syncedAccelerometerData.get(i);
            double dT = (i == 0) ? 0 : Gyrodata.timestamp - syncedGyroscopeData.get(i - 1).timestamp;

            // AHRS六轴补偿陀螺原始输出
            float[] gyrodata = {(float) Gyrodata.x, (float) Gyrodata.y, (float) Gyrodata.z};
            float[] accdata  = {(float) Accdata.x, (float) Accdata.y, (float) Accdata.z};
            gyrodata = ahrs(quaternion, gyrodata, accdata,eInt, (float) dT);

            // 更新四元数
            quaternion = updateQuaternionRK4(quaternion, gyrodata[0], gyrodata[1], gyrodata[2], (float) dT);

            // 计算航向角
            float headingAngle = calculateHeadingAngle(quaternion);
            // 创建航向数据对象并添加到列表中
            headingDataList.add(new HeadingData(Gyrodata.timestamp, headingAngle));
        }
        return headingDataList;
    }
    private float[] ahrs(float[] q, float[] gyroOutputOld, float[] accOutput, float[] eInt, float dT) {

        final float Kp = 1.0f; // 比例增益
        final float Ki = 0.005f; // 积分增益

        // 将四元数转换为旋转矩阵
        float[][] Cbn = quaternionToRotationMatrix(q);
        float[][] Cnb = {
                {Cbn[0][0], Cbn[1][0], Cbn[2][0]},
                {Cbn[0][1], Cbn[1][1], Cbn[2][1]},
                {Cbn[0][2], Cbn[1][2], Cbn[2][2]}
        };

        // 重力向量转换到机体坐标系
        float[] gravityVector = {0, 0, -1};
        float[] v = new float[3];
        for (int i = 0; i < 3; i++) {
            v[i] = 0; // 初始化结果向量的元素为0
            for (int j = 0; j < 3; j++) {
                v[i] += Cnb[i][j] * gravityVector[j];
            }
        }
        // 此时 v 就是重力向量转换到机体坐标系下的结果

        // 加速度计输出归一化
        float a_norm = (float) Math.sqrt(accOutput[0] * accOutput[0] +
                accOutput[1] * accOutput[1] +
                accOutput[2] * accOutput[2]);
        float ax = accOutput[0] / a_norm;
        float ay = accOutput[1] / a_norm;
        float az = accOutput[2] / a_norm;

        // 补偿向量
        float[] e = new float[3];
        e[0] = ay * v[2] - az * v[1];
        e[1] = az * v[0] - ax * v[2];
        e[2] = ax * v[1] - ay * v[0];

        // 记录补偿向量随时间的累积量
        eInt[0] += e[0] * dT;
        eInt[1] += e[1] * dT;
        eInt[2] += e[2] * dT;

        // 补偿陀螺输出
        float[] gyroOutputNew = new float[3];
        gyroOutputNew[0] = gyroOutputOld[0] + Kp * e[0] + Ki * eInt[0];
        gyroOutputNew[1] = gyroOutputOld[1] + Kp * e[1] + Ki * eInt[1];
        gyroOutputNew[2] = gyroOutputOld[2] + Kp * e[2] + Ki * eInt[2];

        return gyroOutputNew;
    }
    private float[][] quaternionToRotationMatrix(float[] q) {
        // 本函数转化所得为Cbn，即可将b系的坐标转化到n系中去
        // 提取四元数的各个分量
        float w = q[0];
        float x = q[1];
        float y = q[2];
        float z = q[3];

        // 计算旋转矩阵的各个元素
        float R11 = 1 - 2 * (y * y + z * z);
        float R12 = 2 * (x * y - z * w);
        float R13 = 2 * (x * z + y * w);

        float R21 = 2 * (x * y + z * w);
        float R22 = 1 - 2 * (x * x + z * z);
        float R23 = 2 * (y * z - x * w);

        float R31 = 2 * (x * z - y * w);
        float R32 = 2 * (y * z + x * w);
        float R33 = 1 - 2 * (x * x + y * y);

        // 构建旋转矩阵
        float[][] R = {
                {R11, R12, R13},
                {R21, R22, R23},
                {R31, R32, R33}
        };

        return R;
    }
    private float[] eulerToQuaternion(float yaw, float pitch, float roll) {
        float cy = (float) Math.cos(yaw / 2);
        float sy = (float) Math.sin(yaw / 2);
        float cp = (float) Math.cos(pitch / 2);
        float sp = (float) Math.sin(pitch / 2);
        float cr = (float) Math.cos(roll / 2);
        float sr = (float) Math.sin(roll / 2);

        float[] q = new float[4];
        q[0] = cy * cp * cr + sy * sp * sr; // q0
        q[1] = cy * cp * sr - sy * sp * cr; // q1
        q[2] = sy * cp * sr + cy * sp * cr; // q2
        q[3] = sy * cp * cr - cy * sp * sr; // q3

        return q;
    }
    // 计算初始水平姿态角即俯仰角和航向角
    private float[] calculateInitialOrientation(List<SensorData> syncedAccelerometerData) {
        // 初始姿态角列表
        List<Float> pitchList = new ArrayList<>();
        List<Float> rollList = new ArrayList<>();

        // 只计算前2秒的数据
        int sampleRate = 50; // 假定采样频率为50Hz
        int samplesToCalculate = 2 * sampleRate;

        // 遍历加速度计数据计算姿态角
        for (int i = 0; i < Math.min(samplesToCalculate, syncedAccelerometerData.size()); i++) {
            SensorData accData = syncedAccelerometerData.get(i);

            // 从加速度计数据中获取X, Y, Z值
            double accX = accData.x;
            double accY = accData.y;
            double accZ = accData.z;

            // 计算roll和pitch值
            float rollValue = (float) Math.atan2(-accX, -accZ);
            float pitchValue = (float) Math.atan2(accX, Math.sqrt(accY * accY + accZ * accZ));

            // 将计算的值添加到列表中
            rollList.add(rollValue);
            pitchList.add(pitchValue);
        }

        // 计算平均的roll和pitch值
        float roll0 = average(rollList);
        float pitch0 = average(pitchList);

        return new float[]{roll0, pitch0};
    }
    // 辅助方法：计算列表中值的平均值
    private float average(List<Float> values) {
        float sum = 0;
        for (Float v : values) {
            sum += v;
        }
        return sum / values.size();
    }
    // 更新四元数的方法，使用四阶龙格-库塔方法
    private float[] updateQuaternionRK4(float[] q, double gx, double gy, double gz, float dT) {
        // 角速度为弧度/秒

        // 计算角速度向量的一半
        double halfGX = 0.5 * gx;
        double halfGY = 0.5 * gy;
        double halfGZ = 0.5 * gz;

        // K1 是四元数的导数
        float[] k1 = qDot(q, halfGX, halfGY, halfGZ);

        // 计算中间值
        float[] q2 = new float[4];
        for (int i = 0; i < 4; i++) {
            q2[i] = q[i] + k1[i] * (dT / 2);
        }

        // K2 是中间值的导数
        float[] k2 = qDot(q2, halfGX, halfGY, halfGZ);

        // 计算另一个中间值
        float[] q3 = new float[4];
        for (int i = 0; i < 4; i++) {
            q3[i] = q[i] + k2[i] * (dT / 2);
        }

        // K3 是第二个中间值的导数
        float[] k3 = qDot(q3, halfGX, halfGY, halfGZ);

        // 计算终值
        float[] q4 = new float[4];
        for (int i = 0; i < 4; i++) {
            q4[i] = q[i] + k3[i] * dT;
        }

        // K4 是终值的导数
        float[] k4 = qDot(q4, halfGX, halfGY, halfGZ);

        // 综合 K1, K2, K3 和 K4 来得到最终的四元数更新值
        float[] qNew = new float[4];
        for (int i = 0; i < 4; i++) {
            qNew[i] = q[i] + (dT / 6) * (k1[i] + 2 * k2[i] + 2 * k3[i] + k4[i]);
        }

        // 四元数归一化
        float norm = (float)Math.sqrt(qNew[0] * qNew[0] + qNew[1] * qNew[1] + qNew[2] * qNew[2] + qNew[3] * qNew[3]);
        for (int i = 0; i < 4; i++) {
            qNew[i] /= norm;
        }

        return qNew;
    }
    //计算四元数的导数的方法
    float[] qDot(float[] quaternion, double hx, double hy, double hz) {
        return new float[]{
                (float)(-hx * quaternion[1] - hy * quaternion[2] - hz * quaternion[3]),
                (float)(hx * quaternion[0] + hz * quaternion[2] - hy * quaternion[3]),
                (float)(hy * quaternion[0] - hz * quaternion[1] + hx * quaternion[3]),
                (float)(hz * quaternion[0] + hy * quaternion[1] - hx * quaternion[2])
        };
    }
    // 计算航向角的方法
    private float calculateHeadingAngle(float[] quaternion) {
        double w=quaternion[0];
        double q1=quaternion[1];
        double q2=quaternion[2];
        double q3=quaternion[3];


        double yaw=Math.atan2(2*(q1*q2+q3*w),(1-2*(q2*q2+q3*q3)));

        /*
        // 微调四元数，使其适配安卓API
        float[] adjustedQuaternion = new float[]{
                quaternion[3], // w (实部)
                quaternion[0], // x (虚部)
                quaternion[1], // y (虚部)
                quaternion[2]  // z (虚部)
        };

        // 使用调整后的四元数计算旋转矩阵
        float[] rotationMatrix = new float[9];
        float[] orientationAngles = new float[3];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, adjustedQuaternion);

        // 获取方向
        SensorManager.getOrientation(rotationMatrix, orientationAngles);
        orientationAngles[0]= (float) Math.atan2(rotationMatrix[3],rotationMatrix[0]);


        // 航向角是orientationAngles数组的第一个元素
        return orientationAngles[0];

         */
        return (float) yaw;
    }



    /***** 数据预处理--滤波降噪函数 *****/
    public List<SensorData> filterSensorData(List<SensorData> originalData, int windowSize) {
        if (originalData == null || originalData.isEmpty() || windowSize <= 0) {
            Log.w(TAG, "原始数据为空，或窗口大小无效。");
            return null;
        }

        List<SensorData> filteredData = new ArrayList<>();
        int dataSize = originalData.size();

        // 遍历每个数据点
        for (int i = 0; i < dataSize; i++) {
            // 初始化累加器
            double sumX = 0, sumY = 0, sumZ = 0;
            int countX = 0, countY = 0, countZ = 0;

            // 确定窗口的范围
            int start = Math.max(i - windowSize, 0);
            int end = Math.min(i + windowSize + 1, dataSize);

            // 创建窗口内数据点的数组
            List<Double> windowX = new ArrayList<>();
            List<Double> windowY = new ArrayList<>();
            List<Double> windowZ = new ArrayList<>();

            // 将窗口内的数据点添加到列表
            for (int j = start; j < end; j++) {
                windowX.add(originalData.get(j).x);
                windowY.add(originalData.get(j).y);
                windowZ.add(originalData.get(j).z);
            }

            // 对每个轴排序并移除最大值和最小值
            Collections.sort(windowX);
            Collections.sort(windowY);
            Collections.sort(windowZ);

            if (windowX.size() > 2) {
                windowX.remove(windowX.size() - 1); // 移除最大值
                windowX.remove(0); // 移除最小值
            }
            if (windowY.size() > 2) {
                windowY.remove(windowY.size() - 1); // 移除最大值
                windowY.remove(0); // 移除最小值
            }
            if (windowZ.size() > 2) {
                windowZ.remove(windowZ.size() - 1); // 移除最大值
                windowZ.remove(0); // 移除最小值
            }

            // 计算平均值
            for (Double x : windowX) {
                sumX += x;
                countX++;
            }
            for (Double y : windowY) {
                sumY += y;
                countY++;
            }
            for (Double z : windowZ) {
                sumZ += z;
                countZ++;
            }

            // 如果计数大于0，则计算平均，否则保持原始值
            double avgX = (countX > 0) ? sumX / countX : originalData.get(i).x;
            double avgY = (countY > 0) ? sumY / countY : originalData.get(i).y;
            double avgZ = (countZ > 0) ? sumZ / countZ : originalData.get(i).z;

            // 添加到滤波后的数据列表
            filteredData.add(new SensorData(originalData.get(i).sensorType, originalData.get(i).timestamp, avgX, avgY, avgZ));
        }

        return filteredData;
    }


    /***** 数据预处理--时间同步函数 *****/
    public void synchronizeSensorData(List<SensorData> accelerometerData,
                                      List<SensorData> gyroscopeData,
                                      List<SensorData> magnetometerData,
                                      List<SensorData> syncedAccelerometerData,
                                      List<SensorData> syncedGyroscopeData,
                                      List<SensorData> syncedMagnetometerData) {
        // 清空传入的同步列表
        syncedAccelerometerData.clear();
        syncedGyroscopeData.clear();
        syncedMagnetometerData.clear();

        // 遍历加速度计数据的时间戳
        for (SensorData accelData : accelerometerData) {
            double accelTimestamp = accelData.timestamp;

            // 对陀螺仪和磁力计数据进行线性插值
            SensorData gyroInterp = interpolateSensorData(gyroscopeData, accelTimestamp);
            SensorData magInterp = interpolateSensorData(magnetometerData, accelTimestamp);

            // 只有当两种插值都成功时，才将加速度计的数据点添加到同步列表中
            if (gyroInterp != null && magInterp != null) {
                syncedAccelerometerData.add(accelData);
                syncedGyroscopeData.add(gyroInterp);
                syncedMagnetometerData.add(magInterp);
            }
        }
    }

    private SensorData interpolateSensorData(List<SensorData> sensorDataList, double targetTimestamp) {
        SensorData before = null;
        SensorData after = null;

        // 寻找目标时间戳前后的数据点
        for (int i = 0; i < sensorDataList.size(); i++) {
            SensorData currentData = sensorDataList.get(i);
            if (currentData.timestamp <= targetTimestamp) {
                before = currentData;
            }
            if (currentData.timestamp > targetTimestamp) {
                after = currentData;
                break;
            }
        }

        // 如果找到了前后数据点，则进行线性插值
        if (before != null && after != null) {
            double ratio = (targetTimestamp - before.timestamp) / (after.timestamp - before.timestamp);
            double xInterp = before.x + ratio * (after.x - before.x);
            double yInterp = before.y + ratio * (after.y - before.y);
            double zInterp = before.z + ratio * (after.z - before.z);
            return new SensorData(before.sensorType, targetTimestamp, xInterp, yInterp, zInterp);
        }

        // 如果没有有效的前后数据点，返回null
        return null;
    }




}