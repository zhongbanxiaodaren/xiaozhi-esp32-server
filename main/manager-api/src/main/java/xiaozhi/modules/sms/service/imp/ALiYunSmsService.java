package xiaozhi.modules.sms.service.imp;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20180501.models.SendMessageToGlobeResponse;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.exception.RenException;
import xiaozhi.common.redis.RedisKeys;
import xiaozhi.common.redis.RedisUtils;
import xiaozhi.modules.sms.service.SmsService;
import xiaozhi.modules.sys.service.SysDictDataService;
import xiaozhi.modules.sys.service.SysParamsService;
import xiaozhi.modules.sys.vo.SysDictDataItem;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class ALiYunSmsService implements SmsService {
    private final SysParamsService  sysParamsService;
    private final RedisUtils redisUtils;
    private final SysDictDataService sysDictDataService;


    @Override
    public void sendVerificationCodeSms(String phone, String VerificationCode) {
        try {
            if (phone.substring(0,3).contains("+86")) {
                Client client = create20170525Client();
                String SignName = sysParamsService.getValue(Constant.SysMSMParam
                        .ALIYUN_SMS_SIGN_NAME.getValue(),true);
                String TemplateCode = sysParamsService.getValue(Constant.SysMSMParam
                        .ALIYUN_SMS_SMS_CODE_TEMPLATE_CODE.getValue(),true);
                SendSmsRequest sendSmsRequest = new SendSmsRequest()
                        .setSignName(SignName)
                        .setTemplateCode(TemplateCode)
                        .setPhoneNumbers(phone)
                        .setTemplateParam(String.format("{\"code\":\"%s\"}", VerificationCode));
                RuntimeOptions runtime = new RuntimeOptions();
                // 复制代码运行请自行打印 API 的返回值
                SendSmsResponse sendSmsResponse = client.sendSmsWithOptions(sendSmsRequest, runtime);
                log.info("国内发送短信响应的requestID: {}", sendSmsResponse.getBody().getRequestId());
            }else {
                // 获取手机号码的国际代号
                String countryCode = getCountryCode(phone);
                // 短信内容
                String Message = "verification code:" + VerificationCode;
                // 短信发送人
                String from = sysParamsService.getValue(
                        Constant.SysMSMParam.getSenderID(countryCode),true);
                SendMessageToGlobeResponse sendMessageToGlobeResponse = sendOverseasSMS(phone,from,Message);
                log.info("国际发送短信响应的requestID: {}", sendMessageToGlobeResponse.getBody().getRequestId());
            }

        } catch (RenException e){
            throw new RenException(e.getMessage());
        } catch (Exception e) {
            // 如果发送失败了退还这次发送数
            String todayCountKey = RedisKeys.getSMSTodayCountKey(phone);
            redisUtils.delete(todayCountKey);
            // 错误 message
            log.error(e.getMessage());
            throw new RenException("短信发送失败");
        }

    }

    /**
     * 发送境外短信（除了中国以往的）
     * @param to 接收的手机号码 国际号+手机号
     * @param from 发送方号码，去阿里云申请对应国家SenderID,非必要的，
     *             参考https://sms-intl.console.aliyun.com/messageProductUserGuide/country/3?spm=a2c63.p38356.0.0.85936232HfwbI5
     * @param Message 自定义的发送消息
     * @return 返回发送后的响应结果
     * @throws Exception 所有异常
     */
    private SendMessageToGlobeResponse sendOverseasSMS(String to,String from,String Message) throws Exception {
        com.aliyun.dysmsapi20180501.Client client = create20180501Client();
        com.aliyun.dysmsapi20180501.models.SendMessageToGlobeRequest sendMessageToGlobeRequest = new com.aliyun.dysmsapi20180501.models.SendMessageToGlobeRequest()
                .setTo(to)
                .setFrom(from)
                .setMessage(Message);
        com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions();
        return client.sendMessageToGlobeWithOptions(sendMessageToGlobeRequest, runtime);
    }



    /**
     * 创建阿里云20170525连接
     * @return 返回连接对象
     */
    private Client create20170525Client(){
        String ACCESS_KEY_ID = sysParamsService.getValue(Constant.SysMSMParam
                .ALIYUN_SMS_ACCESS_KEY_ID.getValue(),true);
        String ACCESS_KEY_SECRET = sysParamsService.getValue(Constant.SysMSMParam
                .ALIYUN_SMS_ACCESS_KEY_SECRET.getValue(),true);
        try {
            Config config = new Config()
                    .setAccessKeyId(ACCESS_KEY_ID)
                    .setAccessKeySecret(ACCESS_KEY_SECRET);
            // 配置 Endpoint。中国站请使用dysmsapi.aliyuncs.com
            config.endpoint = "dysmsapi.aliyuncs.com";
            return new Client(config);
        }catch (Exception e){
            // 错误 message
            log.error(e.getMessage());
            throw new RenException("短信连接建立失败");
        }
    }

    /**
     * 创建阿里云20180501连接
     * @return 返回连接对象
     */
    private com.aliyun.dysmsapi20180501.Client create20180501Client(){
        String ACCESS_KEY_ID = sysParamsService.getValue(Constant.SysMSMParam
                .ALIYUN_SMS_ACCESS_KEY_ID.getValue(),true);
        String ACCESS_KEY_SECRET = sysParamsService.getValue(Constant.SysMSMParam
                .ALIYUN_SMS_ACCESS_KEY_SECRET.getValue(),true);
        try {
            Config config = new Config()
                    .setAccessKeyId(ACCESS_KEY_ID)
                    .setAccessKeySecret(ACCESS_KEY_SECRET);
            // 配置 Endpoint。中国站请使用dysmsapi.aliyuncs.com
            config.endpoint = "dysmsapi.aliyuncs.com";
            return new com.aliyun.dysmsapi20180501.Client(config);
        }catch (Exception e){
            // 错误 message
            log.error(e.getMessage());
            throw new RenException("短信连接建立失败");
        }
    }

    private String getCountryCode(String phone){
        List<SysDictDataItem> list = sysDictDataService.getDictDataByType("MOBILE_AREA");
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber phoneNumberProto = phoneNumberUtil.parse(phone, null);
            String countryCode = "+" + phoneNumberProto.getCountryCode();
            for (SysDictDataItem item : list) {
                if (countryCode.equals(item.getKey())) {
                    return countryCode;
                }
            }
            throw new RenException("系统不支持此国家的手机号码");
        } catch (NumberParseException e) {
            System.err.println("号码解析错误：" + e.getMessage());
            return null;
        }
    }
}
