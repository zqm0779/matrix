/*
 * Tencent is pleased to support the open source community by making wechat-matrix available.
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#import "WCBlockTypeDef.h"
#import "KSStackCursor_Backtrace.h"

@interface WCGetCallStackReportHandler : NSObject

+ (NSData *)getReportJsonDataWithCallStackString:(NSString *)callStackString
                                    withReportID:(NSString *)reportID
                                    withDumpType:(EDumpType)dumpType
                                       withScene:(NSString *)scene;

+ (NSData *)getReportJsonDataWithPowerConsumeStack:(NSArray<NSDictionary *> *)PowerConsumeStackArray
                                      withReportID:(NSString *)reportID
                                      withDumpType:(EDumpType)dumpType;

+ (NSData *)getReportJsonDataWithDiskIOStack:(NSArray<NSDictionary *> *)stackArray
                              withCustomInfo:(NSDictionary *)customInfo
                                withReportID:(NSString *)reportID
                                withDumpType:(EDumpType)dumpType;

+ (NSData *)getReportJsonDataWithFPSStack:(NSArray<NSDictionary *> *)stackArray
                           withCustomInfo:(NSDictionary *)customInfo
                             withReportID:(NSString *)reportID
                             withDumpType:(EDumpType)dumpType;

+ (NSData *)getReportJsonDataWithLagProfileStack:(NSArray<NSDictionary *> *)stackArray;

@end
