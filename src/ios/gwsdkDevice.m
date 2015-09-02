/********* gwsdkwrapper.m Cordova Plugin Implementation *******/

#import <Cordova/CDV.h>
#import <SystemConfiguration/CaptiveNetwork.h>
#import <XPGWifiSDK/XPGWifiSDK.h>

@interface gwsdkDevice : CDVPlugin<XPGWifiDeviceDelegate,XPGWifiSDKDelegate> {
    
    // Member variables go here.
    NSString * productKey,* _appid,* _uid,* _token,* _mac;
    NSMutableDictionary * _value;
    NSArray * _deviceList;//当前用户的设备列表
    Boolean * controlState;//锁定控制状态，只能发一次
    /*是否登录*/
    BOOL isDiscoverLock;
    CDVPluginResult *pluginResult;
}
- (void)discover:(CDVInvokedUrlCommand *)command;


@property (strong,nonatomic) CDVInvokedUrlCommand * commandHolder;

@end

@implementation gwsdkDevice

@synthesize commandHolder;



-(void)pluginInitialize{
    
}

-(void) deviceControl:(CDVInvokedUrlCommand *)command{
    _appid=command.arguments[0];
    productKey=command.arguments[1];
    _uid=command.arguments[2];
    _token=command.arguments[3];
    _mac=command.arguments[4];
    _value=command.arguments[5];
    isDiscoverLock=true;
    [self initCordova];//初始化设置appid
    [self discover:command];
}
-(void)initCordova{
    if(_appid){
        [XPGWifiSDK startWithAppID:_appid];
        [XPGWifiSDK sharedInstance].delegate = self;
    }
}
/**
 根据 uid，token，appId 获取设备列表
 */
-(void) discover:(CDVInvokedUrlCommand *)command
{
    
    self.commandHolder = command;//???这个方法是做什么的？
    /**
     * @brief 获取绑定设备及本地设备列表
     * @param uid：登录成功后得到的uid
     * @param token：登录成功后得到的token
     * @param specialProductKey：指定待筛选设备的产品标识（获取或搜索到未指定设备产品标识的设备将其过滤，指定Nil则不过滤）
     * @see 对应的回调接口：[XPGWifiSDK XPGWifiSDK:didDiscovered:result:]
     */
    [[XPGWifiSDK sharedInstance] getBoundDevicesWithUid:_uid token:_token specialProductKeys:productKey,nil];
}

/**
 devicelogin 的回调
 根据mac 查找设备，如果查找成功－－》登录device
 **/
- (void)XPGWifiSDK:(XPGWifiSDK *)wifiSDK didDiscovered:(NSArray *)deviceList result:(int)result
{
    if(isDiscoverLock){//如果锁定状态为true 那么就是控制命令已经发送成功
    if(deviceList.count>0&&result==0){
        _deviceList=deviceList;
        [self deviceLogin:deviceList];
    }
    }
}
/**
 筛选要控制的device，判断是否登录。
 如果这个device没有登录，则执行login方法登录device。
 **/
- (void)deviceLogin:(NSArray *)deviceList{
    for(XPGWifiDevice *device in deviceList){
        if (device.macAddress==_mac) {
            isDiscoverLock=false;//设置锁定状态
            if (device.isConnected) {
                [self cWrite:device];
            }else{
                [device login:_uid token:_token];
            }
        }
    }
}
/*!
 login device 的回调
 判断这个device是否登录成功，如果成功则发送控制命令
 !*/
- (void)XPGWifiDevice:(XPGWifiDevice *)device didLogin:(int)result
{
        if(result == 0&&device){
            [self  cWrite:device];
            }
}
/**
 发送控制命令
 */
-(void) cWrite:(XPGWifiDevice *)device{
    NSDictionary *data=nil;
    @try {
        data=@{@"cmd":@1,@"entity0":_value};
        NSLog(@"Write data: %@", data);
        [device write:data];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"success"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:commandHolder.callbackId];
        
    }
    @catch (NSException *exception) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[exception reason]];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:commandHolder.callbackId];
        
    }
}
/**
 write 的回调，
 这里判断发送消息是否成功
 **/
- (BOOL)XPGWifiDevice:(XPGWifiDevice *)device didReceiveData:(NSDictionary *)data result:(int)result
{
    /**
     * 数据部分
     */
    NSDictionary *_data = [data valueForKey:@"data"];
    NSMutableArray *rows = [NSMutableArray array];
    
}

@end
