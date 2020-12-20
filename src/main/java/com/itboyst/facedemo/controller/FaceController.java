package com.itboyst.facedemo.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.enums.ImageFormat;
import com.arcsoft.face.toolkit.ImageFactory;
import com.arcsoft.face.toolkit.ImageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.itboyst.facedemo.dto.FaceDetectResDTO;
import com.itboyst.facedemo.dto.FaceRecognitionResDTO;
import com.itboyst.facedemo.entity.ProcessInfo;
import com.itboyst.facedemo.entity.UserCompareInfo;
import com.itboyst.facedemo.rpc.Response;
import com.itboyst.facedemo.service.FaceEngineService;
import com.itboyst.facedemo.util.Base64Util;
import com.itboyst.facedemo.util.HistoryRamCache;
import com.itboyst.facedemo.util.UserRamCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import javax.annotation.PostConstruct;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;


@Controller
@Slf4j
public class FaceController {

    @Autowired
    private FaceEngineService faceEngineService;

    @Value("${server.port}")
    private int port;


    //初始化注册人脸，注册到本地内存
    @PostConstruct
    public void initFace() throws FileNotFoundException {
        Map<String, String> fileMap = Maps.newHashMap();
        fileMap.put("zhao1", "赵丽颖");
        fileMap.put("yang1", "杨紫");

        for (String f : fileMap.keySet()) {
            ClassPathResource resource = new ClassPathResource("static/images/" + f +  ".jpg");
            InputStream inputStream = null;
            try {
                inputStream = resource.getInputStream();
            } catch (IOException e) {
            }
            ImageInfo rgbData = ImageFactory.getRGBData(inputStream);
            List<FaceInfo> faceInfoList = faceEngineService.detectFaces(rgbData);
            if (CollectionUtil.isNotEmpty(faceInfoList)) {
                byte[] feature = faceEngineService.extractFaceFeature(rgbData, faceInfoList.get(0));
                UserRamCache.UserInfo userInfo = new UserCompareInfo();
                userInfo.setFaceId(f);
                userInfo.setName(fileMap.get(f));
                userInfo.setFaceFeature(feature);
                UserRamCache.addUser(userInfo);
                HistoryRamCache.addHistory(f);
            }
        }

        log.info("http://127.0.0.1:" + port + "/");

    }


    /*
    人脸添加
     */
    @RequestMapping(value = "/faceAdd", method = RequestMethod.POST)
    @ResponseBody
    public Response faceAdd(String file, String faceId, String name) {
        return null;
    }

    /*
    人脸识别
     */
    @RequestMapping(value = "/faceRecognition", method = RequestMethod.POST)
    @ResponseBody
    public Response<List<FaceRecognitionResDTO>> faceRecognition(String image) {

        List<FaceRecognitionResDTO> faceRecognitionResDTOList = Lists.newLinkedList();
        byte[] bytes = Base64Util.base64ToBytes(image);
        ImageInfo rgbData = ImageFactory.getRGBData(bytes);
        List<FaceInfo> faceInfoList = faceEngineService.detectFaces(rgbData);
        if (CollectionUtil.isNotEmpty(faceInfoList)) {
            for (FaceInfo faceInfo : faceInfoList) {
                FaceRecognitionResDTO faceRecognitionResDTO = new FaceRecognitionResDTO();
                faceRecognitionResDTO.setRect(faceInfo.getRect());
                byte[] feature = faceEngineService.extractFaceFeature(rgbData, faceInfo);
                if (feature != null) {
                    List<UserCompareInfo> userCompareInfos = faceEngineService.faceRecognition(feature, UserRamCache.getUserList(), 0.8f);
                    if (CollectionUtil.isNotEmpty(userCompareInfos)) {
                        faceRecognitionResDTO.setName(userCompareInfos.get(0).getName());
                        faceRecognitionResDTO.setSimilar(userCompareInfos.get(0).getSimilar());
                    }
                }
                faceRecognitionResDTOList.add(faceRecognitionResDTO);
            }

        }


        return Response.newSuccessResponse(faceRecognitionResDTOList);
    }


    @RequestMapping(value = "/detectFaces", method = RequestMethod.POST)
    @ResponseBody
    public Response<List<FaceDetectResDTO>> detectFaces(String image) {

        byte[] bytes = Base64Util.base64ToBytes(image);
        ImageInfo rgbData = ImageFactory.getRGBData(bytes);
        List<FaceDetectResDTO> faceDetectResDTOS = Lists.newLinkedList();
        List<FaceInfo> faceInfoList = faceEngineService.detectFaces(rgbData);
        if (CollectionUtil.isNotEmpty(faceInfoList)) {
            List<ProcessInfo> process = faceEngineService.process(rgbData, faceInfoList);

            for (int i = 0; i < faceInfoList.size(); i++) {
                FaceDetectResDTO faceDetectResDTO = new FaceDetectResDTO();
                FaceInfo faceInfo = faceInfoList.get(i);
                faceDetectResDTO.setRect(faceInfo.getRect());
                faceDetectResDTO.setOrient(faceInfo.getOrient());
                faceDetectResDTO.setFaceId(faceInfo.getFaceId());
                if (CollectionUtil.isNotEmpty(process)) {
                    ProcessInfo processInfo = process.get(i);
                    faceDetectResDTO.setAge(processInfo.getAge());
                    faceDetectResDTO.setGender(processInfo.getGender());
                    faceDetectResDTO.setLiveness(processInfo.getLiveness());

                }
                faceDetectResDTOS.add(faceDetectResDTO);

            }
        }

        return Response.newSuccessResponse(faceDetectResDTOS);
    }

    @RequestMapping(value = "/compareFaces", method = RequestMethod.POST)
    @ResponseBody
    public Response<Float> compareFaces(String image1, String image2) {

        byte[] bytes1 = Base64Util.base64ToBytes(image1);
        byte[] bytes2 = Base64Util.base64ToBytes(image2);
        ImageInfo rgbData1 = ImageFactory.getRGBData(bytes1);
        ImageInfo rgbData2 = ImageFactory.getRGBData(bytes2);

        Float similar = faceEngineService.compareFace(rgbData1, rgbData2);

        return Response.newSuccessResponse(similar);
    }


    /**
     * 签到接口，传入图片 流程 图片解码 人脸提取 特征提取 存储
     * @author iLoveCYaRon Blade Xu
     * @time 2020/12/19 22:27
     * @param image 人脸图片，Base64数据，带形如“data:image/jpg;base64,”请求头
     * @param id 要签到的人脸ID，目前是名字
     * @return 匹配成功 返回 {data:true}
     */
    //TODO: 修改返回值格式
    @RequestMapping(value = "/sign", method = RequestMethod.POST)
    @ResponseBody
    public Response<Boolean> signWithImage(String image, String id) {
        if(UserRamCache.getUserById(id) == null) {
            return Response.newSuccessResponse(false);
        }

        //将字符串解码回二进制图片数据
        byte[] bytes = Base64Util.base64ToBytes(image);
        ImageInfo rgbData = ImageFactory.getRGBData(bytes);
      
        return Response.newSuccessResponse(getSignResult(id, rgbData));
    }

    /**
     * 签到接口，传入图片 流程 人脸提取 特征提取 存储
     * @author iLoveCYaRon Blade Xu
     * @time 2020/12/19 22:27
     * @param imageData 经ImageFactory处理过的bgr24数据
     * @param height 图像高度
     * @param width 图像宽度
     * @param id 要签到的人脸ID，目前是名字
     * @return 匹配成功 返回 {data:true}
     */
    @RequestMapping(value = "/sign2", method = RequestMethod.POST)
    @ResponseBody
    public Response<Boolean> signWithImageData(String imageData,int width, int height, String id) throws IOException, ClassNotFoundException {
        if(UserRamCache.getUserById(id) == null) {
            return Response.newSuccessResponse(false);
        }

        return Response.newSuccessResponse(getSignResult(id, processImageInfo(imageData, width, height)));
    }

    //将传入的数据处理为ImageInfo对象
    private ImageInfo processImageInfo(String imageData,int width, int height) {
        ImageInfo info = new ImageInfo();
        info.setImageData(Base64Util.base64ToBytes(imageData));
        info.setWidth(width);
        info.setHeight(height);
        //HardCode
        info.setImageFormat(ImageFormat.CP_PAF_BGR24);
        return info;
    }

    /**
     * 获取签到结果
     * @author iLoveCYaRon Blade Xu
     * @time 2020/12/20 14:25
     * @param id 签到人名称
     * @param imageInfo 经过处理提取的图像数据 bgr24格式
     * @return 签到是否成功
     */
    private Boolean getSignResult(String id, ImageInfo imageInfo) {

        //检测提取人脸特征，未提取到人脸或者人脸不匹配返回false
        List<FaceInfo> faceInfos = faceEngineService.detectFaces(imageInfo);
        if (!faceInfos.isEmpty()) {
            byte[] feature = faceEngineService.extractFaceFeature(imageInfo, faceInfos.get(0));
            if (faceEngineService.faceRecognition(feature, UserRamCache.getUserById(id), 0.8f) != null) {

                //获取当前系统时间
                Date date = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String day = dateFormat.format(date);
                //为该id的签到历史添加一项
                HistoryRamCache.addHistoryItem(id, new HistoryRamCache.History(id, day));

                return true;
            }
        }
        return false;
    }

    /**
     * 获取签到历史接口
     * @author HardAlways
     * @time 2020/12/20 11:17
     * @param id 人名
     * @return 返回 {data:签到历史} 签到历史可空
     */
    @RequestMapping(value = "/historyList", method = RequestMethod.GET)
    @ResponseBody
    public Response<List<HistoryRamCache.History>> historyList(String id) {
        if(HistoryRamCache.getHistoryList(id) == null) {
            return Response.newSuccessResponse(null);
        }
        return Response.newSuccessResponse(HistoryRamCache.getHistoryList(id));
    }

    /**
     * 获取学生列表接口
     * @author HardAlways
     * @time 2020/12/20 13：10
     * @return 返回 {data:学生列表}
     */
    @RequestMapping(value = "/studentList", method = RequestMethod.GET)
    @ResponseBody
    public Response<List<UserRamCache.UserInfo>> studentList() {
        if (UserRamCache.getUserList() == null) {
            return Response.newSuccessResponse(null);
        }
        return Response.newSuccessResponse(UserRamCache.getUserList());
    }
  
    /* 注册接口1 */
    //TODO: 检查ID是否已存在
    @RequestMapping(value = "/register1", method = RequestMethod.POST)
    @ResponseBody
    public Response<Map<String,String>> registerWithImage(String image, String id) throws IOException {
        //将字符串解码回二进制图片数据
        byte[] bytes = Base64Util.base64ToBytes(image);
        ImageInfo rgbData = ImageFactory.getRGBData(bytes);

        return Response.newSuccessResponse(getRegisterResult(id, rgbData));
    }

    /* 注册接口2 */
    @RequestMapping(value = "/register2", method = RequestMethod.POST)
    @ResponseBody
    public Response<Map<String,String>> registerWithData(String imageData,int width, int height, String id) throws IOException {
        //将字符串处理为imageInfo
        ImageInfo imageInfo = processImageInfo(imageData, width, height);

        return Response.newSuccessResponse(getRegisterResult(id, imageInfo));
    }

    /* 获取注册结果 */
    private Map<String,String> getRegisterResult(String id, ImageInfo imageInfo) throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        //检测提取人脸特征，未提取到人脸返回fail
        List<FaceInfo> faceInfos = faceEngineService.detectFaces(imageInfo);
        if (!faceInfos.isEmpty()) {
            byte[] feature = faceEngineService.extractFaceFeature(imageInfo, faceInfos.get(0));

            UserRamCache.UserInfo userInfo = new UserRamCache.UserInfo();
            userInfo.setFaceId(id);
            userInfo.setName(id);
            userInfo.setFaceFeature(feature);
            UserRamCache.addUser(userInfo);

            //将人脸图片以字符串形式保存到本地
            String fileName = id;
            String filePath = this.getClass().getResource("/").getPath();
            File record = new File(filePath,fileName);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(record)));
            writer.write(imageInfo.toString());
            writer.flush();
            writer.close();
            map.put("success", "true");
        }
        map.put("fail", "false");
        return map;
    }
}
