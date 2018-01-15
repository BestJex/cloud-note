package cn.edu.jit.global;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 全局方法
 * @author jitwxs
 * @date 2018/1/2 22:33
 */
public class GlobalFunction {

    /**
     * 获取UUID
     * @return 返回UUID
     */
    public static String getUUID() {
        String uuid = UUID.randomUUID().toString();
        return uuid.replaceAll("-", "");
    }

    public static String getDate2Second(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if(date == null) {
            date = new Date();
        }
        return sdf.format(date);
    }

    public static String getDate2Day(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        if(date == null) {
            date = new Date();
        }
        return sdf.format(date);
    }

    /**
     * 获取登陆用户手机号码
     */
    public static String getSelfTel() {
        Subject subject = SecurityUtils.getSubject();
        return (String) subject.getPrincipal();
    }


    /**
     * 获取资源在服务器上的真实url
     */
    public static String getRealUrl(String url) {
        int i = url.indexOf("upload");
        if(i == 0) {
            return "";
        } else {
            return GlobalConstant.SER_URL + "/" + url.substring(i);
        }
    }

    /**
     * 创建多级路径
     */
    public static void createDir(String realPath) {
        File file = new File(realPath);
        file.mkdirs();
    }

    /**
     * 删除文件夹及其所有文件
     * @param folderPath 文件夹路径
     */
    public static boolean delFolder(String folderPath) throws Exception{
        //删除完里面所有内容
        boolean flag = delAllFile(folderPath);
        if(!flag) {
            return false;
        }
        File myFilePath = new File(folderPath);
        myFilePath.delete(); //删除空文件夹
        return true;
    }

    private static boolean delAllFile(String path) throws Exception{
        boolean flag = false;
        File file = new File(path);
        if (!file.exists()) {
            return false;
        }
        if (!file.isDirectory()) {
            return false;
        }
        String[] tempList = file.list();
        File temp = null;
        for (int i = 0; i < tempList.length; i++) {
            if (path.endsWith(File.separator)) {
                temp = new File(path + tempList[i]);
            } else {
                temp = new File(path + File.separator + tempList[i]);
            }
            if (temp.isFile()) {
                temp.delete();
            }
            if (temp.isDirectory()) {
                //先删除文件夹里面的文件
                delAllFile(path + "/" + tempList[i]);
                //再删除空文件夹
                delFolder(path + "/" + tempList[i]);
                flag = true;
            }
        }
        return flag;
    }

    /**
     * 删除单个文件
     */
    public static boolean deleteSignalFile(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean renameFile(String path,String oldName,String newName) {
        //新的文件名和以前文件名不同时,才有必要进行重命名
        if (!oldName.equals(newName)) {
            File oldfile = new File(path + "/" + oldName);
            File newfile = new File(path + "/" + newName);
            if (!oldfile.exists()) {
                System.out.println("重命名文件不存在");
                return false;
            }
            //若在该目录下已经有一个文件和新文件名相同，则不允许重命名
            if (newfile.exists()) {
                System.out.println(newName + "已经存在！");
                return false;
            } else {
                oldfile.renameTo(newfile);
            }
        } else {
            System.out.println("新文件名和旧文件名相同...");
            return false;
        }
        return true;
    }

    /**
     * 上传文件
     * @param item FileItem对象
     * @param targetFilePath 要存放的文件路径
     * @throws IOException 流异常
     */
    public static void uploadFile(FileItem item, String targetFilePath) throws IOException{
        // 拷贝数据
        InputStream in = item.getInputStream();
        OutputStream out = new FileOutputStream(targetFilePath);
        IOUtils.copy(in, out);
        in.close();
        out.close();

        // 删除临时文件
        item.delete();
    }

    /**
     * 下载文件
     * @param path 文件真实路径
     * @param out response.getOutputStream()
     * @throws IOException
     */
    public static void downloadFile(String path, ServletOutputStream out) throws IOException{
        InputStream in = new FileInputStream(path);
        int len = 0;
        byte[] buf = new byte[1024];
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
    }

    /**
     * 发送验证码
     */
    public static SendSmsResponse sendSms(String tel, String code) throws ClientException {

        //可自助调整超时时间
        System.setProperty("sun.net.client.defaultConnectTimeout", "10000");
        System.setProperty("sun.net.client.defaultReadTimeout", "10000");

        //初始化acsClient,暂不支持region化
        IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", GlobalConstant.ACCESS_KEY_ID, GlobalConstant.ACCESS_KEY_SECRET);
        DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", GlobalConstant.PRODUCT, GlobalConstant.DOMAIN);
        IAcsClient acsClient = new DefaultAcsClient(profile);

        //组装请求对象-具体描述见控制台-文档部分内容
        SendSmsRequest request = new SendSmsRequest();
        //必填:待发送手机号
        request.setPhoneNumbers(tel);
        //必填:短信签名-可在短信控制台中找到
        request.setSignName(GlobalConstant.LABEL_NAME);
        //必填:短信模板-可在短信控制台中找到
        request.setTemplateCode(GlobalConstant.MOULD_ID);
        //可选:模板中的变量替换JSON串,如模板内容为"亲爱的${name},您的验证码为${code}"时,此处的值为
        request.setTemplateParam("{\"code\":\"" + code + "\"}");

        //选填-上行短信扩展码(无特殊需求用户请忽略此字段)
        //request.setSmsUpExtendCode("90997");

        //可选:outId为提供给业务方扩展字段,最终在短信回执消息中将此值带回给调用者
        request.setOutId("yourOutId");

        //hint 此处可能会抛出异常，注意catch
        SendSmsResponse sendSmsResponse = acsClient.getAcsResponse(request);

        return sendSmsResponse;
    }

    /**
     * 缩略字符串（不区分中英文字符）
     * @param str 目标字符串
     * @param length 截取长度
     * @return
     */
    public static String abbr(String str, int length) {
        if (str == null) {
            return "";
        }
        try {
            StringBuilder sb = new StringBuilder();
            int currentLength = 0;
            for (char c : replaceHtml(StringEscapeUtils.unescapeHtml4(str)).toCharArray()) {
                currentLength += String.valueOf(c).getBytes("GBK").length;
                if (currentLength <= length - 3) {
                    sb.append(c);
                } else {
                    sb.append("...");
                    break;
                }
            }
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 替换掉HTML标签方法
     */
    public static String replaceHtml(String html) {
        if (StringUtils.isBlank(html)){
            return "";
        }
        String regEx = "<.+?>";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(html);
        return m.replaceAll("");
    }

    /**
     * 从Html中提取汉字
     */
    public static String getChineseFromHtml(String htmlStr){
        Document doc = Jsoup.parse(htmlStr);
        //将输入的HTML解析为一个新的文档
        String htmlText=doc.text();
        StringBuilder finalText = new StringBuilder();
        //正则匹配中文及中文标点符号
        String regEx="[\u4E00-\u9FA5|\\！|\\，|\\。|\\（|\\）|\\《|\\》|\\“|\\”|\\？|\\：|\\；|\\【|\\】]";
        Pattern p = Pattern.compile(regEx);

        Matcher m = p.matcher(htmlText);
        while (m.find()) {
            finalText.append(m.group());
        }
        return finalText.toString();
    }

    /**
     * 获得用户远程地址
     */
    public static String getRemoteAddr(HttpServletRequest request){
        String remoteAddr = request.getHeader("X-Real-IP");
        if (!StringUtils.isBlank(remoteAddr)) {
            remoteAddr = request.getHeader("X-Forwarded-For");
        }else if (!StringUtils.isBlank(remoteAddr)) {
            remoteAddr = request.getHeader("Proxy-Client-IP");
        }else if (!StringUtils.isBlank(remoteAddr)) {
            remoteAddr = request.getHeader("WL-Proxy-Client-IP");
        }
        return remoteAddr != null ? remoteAddr : request.getRemoteAddr();
    }

    /**
     * 将ErrorStack转化为String.
     */
    public static String getStackTraceAsString(Throwable e) {
        if (e == null){
            return "";
        }
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
}
