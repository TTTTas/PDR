package com.example.myapplication;

import java.util.ArrayList;
import java.util.List;

import android.hardware.SensorManager;
import android.util.Log;

import android.util.Log;
import java.util.List;

public class PDRProcessor {
    private static final String TAG = "PDRData"; // 定义日志标记

    public class HeadingData {
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
                double x = Double.parseDouble(dataParts[2]);
                double y = Double.parseDouble(dataParts[3]);
                double z = Double.parseDouble(dataParts[4]);

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

        // 使用陀螺仪数据计算航向角和时间戳
        List<HeadingData> headingDataList = calculateOrientationUsingGyroscope(syncedGyroscopeData);

        // 调用步伐探测方法
        List<Double> stepTimestamps = detectSteps(syncedAccelerometerData, 9.8);

        // 调用航迹推算方法
        List<double[]> trajectory = calculateTrajectory(stepTimestamps, headingDataList, 0.3);



        // 打印轨迹点到日志
        for (double[] position : trajectory) {
            double posX = position[0];
            double posY = position[1];
            Log.d(TAG, "X: " + posX + ", Y: " + posY);
        }
        // 返回计算出的二维轨迹坐标
        return trajectory;

    }



    public List<double[]> calculateTrajectory(List<Double> stepsTimestamps, List<HeadingData> eulerAnglesData, double stepLength) {
        double posX = 0;
        double posY = 0;
        List<double[]> positions = new ArrayList<>();
        positions.add(new double[]{posX, posY}); // 存储每一步的位置

        for (double stepTime : stepsTimestamps) {
            // 找到对应的航向角
            double headingAngle = findHeadingAngle(eulerAnglesData, stepTime);

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



    // 计算使用陀螺仪数据的航向角
    private List<HeadingData> calculateOrientationUsingGyroscope(List<SensorData> syncedGyroscopeData) {
        // 初始化四元数
        float[] quaternion = new float[]{1, 0, 0, 0};
        List<HeadingData> headingDataList = new ArrayList<>();

        // 遍历每个陀螺仪数据点
        for (int i = 0; i < syncedGyroscopeData.size(); i++) {
            SensorData dataPoint = syncedGyroscopeData.get(i);
            double dT = (i == 0) ? 0 : dataPoint.timestamp - syncedGyroscopeData.get(i - 1).timestamp;

            // 更新四元数
            quaternion = updateQuaternionRK1(quaternion, dataPoint.x, dataPoint.y, dataPoint.z, (float) dT);

            // 计算航向角
            float headingAngle = calculateHeadingAngle(quaternion);
            // 创建航向数据对象并添加到列表中
            headingDataList.add(new HeadingData(dataPoint.timestamp, headingAngle));
        }
        return headingDataList;
    }

    // 更新四元数的方法，使用一阶龙格-库塔方法
    private float[] updateQuaternionRK1(float[] q, double gx, double gy, double gz, float dT) {
        // 角速度为弧度/秒

        // 计算角速度向量的一半
        double halfGX = 0.5 * gx;
        double halfGY = 0.5 * gy;
        double halfGZ = 0.5 * gz;

        // 计算四元数的导数
        float[] qDot = new float[]{
                (float)(-halfGX * q[1] - halfGY * q[2] - halfGZ * q[3]),
                (float)(halfGX * q[0] + halfGZ * q[2] - halfGY * q[3]),
                (float)(halfGY * q[0] - halfGZ * q[1] + halfGX * q[3]),
                (float)(halfGZ * q[0] + halfGY * q[1] - halfGX * q[2])
        };

        // 使用一阶龙格-库塔方法更新四元数
        float[] qNew = new float[]{
                q[0] + qDot[0] * dT,
                q[1] + qDot[1] * dT,
                q[2] + qDot[2] * dT,
                q[3] + qDot[3] * dT
        };

        // 四元数归一化
        float norm = (float) Math.sqrt(qNew[0] * qNew[0] + qNew[1] * qNew[1] + qNew[2] * qNew[2] + qNew[3] * qNew[3]);
        qNew[0] /= norm;
        qNew[1] /= norm;
        qNew[2] /= norm;
        qNew[3] /= norm;

        return qNew;
    }

    // 计算航向角的方法
    private float calculateHeadingAngle(float[] quaternion) {
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

        // 航向角是orientationAngles数组的第一个元素
        return orientationAngles[0];
    }



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