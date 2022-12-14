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

#ifndef stack_frames_db_h
#define stack_frames_db_h

#include <stdio.h>
#include <mach/mach.h>

struct stack_frames_db;

stack_frames_db *stack_frames_db_open_or_create(const char *db_dir);
void stack_frames_db_close(stack_frames_db *db_context);

uint32_t stack_frames_db_add_stack(stack_frames_db *db_context, uintptr_t *frames, int32_t frames_count, uint64_t stack_hash);
void stack_frames_db_check_stack(stack_frames_db *db_context, uintptr_t *frames, int32_t frames_count, uint64_t stack_hash);
void stack_frames_db_unwind_stack(
stack_frames_db *db_context, uint32_t stack_identifier, uint64_t *out_frames_buffer, uint32_t *out_frames_count, uint32_t max_frames);

#endif /* stack_frames_db_h */
