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
- (void)discover;


@property (strong,nonatomic) CDVInvokedUrlCommand * commandHolder;

@end

@implementation gwsdkDevice

@synthesize commandHolder;



-(void)pluginInitialize{
    
}

-(void) deviceControl:(CDVInvokedUrlCommand *)command{
//    _appid=command.arguments[0];
    productKey=command.arguments[1];
    _uid=command.arguments[2];
    _token=command.arguments[3];
    _mac=command.arguments[4];
    _value=command.arguments[5];//todo: back to value:5
    isDiscoverLock=true;
     self.commandHolder = command;//???这个方法是做什么的？
    [self initCordova:command];//初始化设置appid
    [self discover];
}
-(void)initCordova:(CDVInvokedUrlCommand *)command{
    //如果_appid没有设置，或者appId改变那么设置startWithAppID，这个方法只执行一次
    if(_appid==nil||_appid!=command.arguments[0]){
        _appid=command.arguments[0];
        [XPGWifiSDK startWithAppID:_appid];
        [XPGWifiSDK sharedInstance].delegate = self;
    }
}
/**
 根据 uid，token，appId 获取设备列表
 */
-(void) discover{
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
        if(deviceList.count>0 && result==0){
        
        //_deviceList=deviceList;
            for (int i=0; i<[deviceList count]; i++) {
               // NSLog(@"%@",[deviceList[i] macAddress]);
            }
            
            for (int i=0; i<[deviceList count]; i++) {
                NSLog(@"=======%@",[deviceList[i] macAddress]);
                XPGWifiDevice *device = deviceList[i];
                //[[deviceList[i] macAddress]]
                
                
                if ([device.macAddress isEqualToString: [_mac uppercaseString]]) {
                    isDiscoverLock=false;//设置锁定状态
                    if (device.isConnected) {
                        [self cWrite:deviceList[i]];
                    }else{
                        device.delegate = self;
                        [device login:_uid token:_token];
                        
                    }
                }
            }

//        [self deviceLogin:deviceList];
        }
    }
}
///**
// 筛选要控制的device，判断是否登录。
// 如果这个device没有登录，则执行login方法登录device。
// **/
//- (void)deviceLogin:(NSArray *)deviceList{
//    for(XPGWifiDevice *device in deviceList){
//            
//        if (device.macAddress==_mac) {
//            isDiscoverLock=false;//设置锁定状态
//            if (device.isConnected) {
//                [self cWrite:device];
//            }else{
//                [device login:_uid token:_token];
//            }
//        }
//    }
//}
/*!
 login device 的回调
 判断这个device是否登录成功，如果成功则发送控制命令
 !*/
- (void)XPGWifiDevice:(XPGWifiDevice *)device didLogin:(int)result
{
        if(result == 0 && device){
            [self  cWrite:device];
            }
}
/**
 发送控制命令
 */
-(void) cWrite:(XPGWifiDevice *)device{
    NSDictionary *data=nil;
    NSMutableDictionary * data1 = [NSMutableDictionary dictionaryWithDictionary: _value];
    @try {
        
        
        
        NSEnumerator *enumerator1= [_value keyEnumerator];
        id key=[enumerator1 nextObject];
        while (key) {
            NSString *object=[_value objectForKey:key];
            
            NSData *data =[gwsdkDevice stringToHex:object];
            NSString * encodeStr= [XPGWifiBinary encode:data];
            NSLog(@"%@===%@",object,encodeStr);
            [data1 setObject:encodeStr forKey:key];
           
            key=[enumerator1 nextObject];
        }
        
        data=@{@"cmd":@1,@"entity0":data1};
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

+(NSData *)stringToHex: (NSString *) str{
    //-------------------
    
    // NSString --> hex
    const char *buf = [str UTF8String];
    NSMutableData *data = [NSMutableData data];
    if (buf)
    {
        uint32_t len = strlen(buf);
        
        char singleNumberString[3] = {'\0', '\0', '\0'};
        uint32_t singleNumber = 0;
        for(uint32_t i = 0 ; i < len; i+=2)
        {
            if ( ((i+1) < len) && isxdigit(buf[i]) && (isxdigit(buf[i+1])) )
            {
                singleNumberString[0] = buf[i];
                singleNumberString[1] = buf[i + 1];
                sscanf(singleNumberString, "%x", &singleNumber);
                uint8_t tmp = (uint8_t)(singleNumber & 0x000000FF);
                [data appendBytes:(void *)(&tmp)length:1];
            }
            else
            {
                break;
            }
        }
    }
    //-------------------
    return data;
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
