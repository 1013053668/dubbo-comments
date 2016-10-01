/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.remoting.transport.dispatcher.all;

import java.util.concurrent.ExecutorService;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.ExecutionException;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.transport.dispatcher.ChannelEventRunnable;
import com.alibaba.dubbo.remoting.transport.dispatcher.WrappedChannelHandler;
import com.alibaba.dubbo.remoting.transport.dispatcher.ChannelEventRunnable.ChannelState;
//对于client来说 , send根本不会消耗任何线程, receive会
public class AllChannelHandler extends WrappedChannelHandler {
    
    public AllChannelHandler(ChannelHandler handler, URL url) {
        super(handler, url);
    }

    public void connected(Channel channel) throws RemotingException {
        ExecutorService cexecutor = getExecutorService(); 
        try{
            cexecutor.execute(new ChannelEventRunnable(channel, handler ,ChannelState.CONNECTED));
        }catch (Throwable t) {
            throw new ExecutionException("connect event", channel, getClass()+" error when process connected event ." , t);
        }
    }
    
    public void disconnected(Channel channel) throws RemotingException {
        ExecutorService cexecutor = getExecutorService(); 
        try{
            cexecutor.execute(new ChannelEventRunnable(channel, handler ,ChannelState.DISCONNECTED));
        }catch (Throwable t) {
            throw new ExecutionException("disconnect event", channel, getClass()+" error when process disconnected event ." , t);
        }
    }

    public void received(Channel channel, Object message) throws RemotingException {
        //FIXME 默认为 FixedThreadPool  add by jileng
        ExecutorService cexecutor = getExecutorService();
        //FIXME all模式下所有消息都派发到线程池，包括请求，响应，连接事件，断开事件，心跳等
        try {
            cexecutor.execute(new ChannelEventRunnable(channel, handler, ChannelState.RECEIVED, message));
        } catch (Throwable t) {
            throw new ExecutionException(message, channel, getClass() + " error when process received event .", t);
        }
    }

    public void caught(Channel channel, Throwable exception) throws RemotingException {
        ExecutorService cexecutor = getExecutorService(); 
        try{
            cexecutor.execute(new ChannelEventRunnable(channel, handler ,ChannelState.CAUGHT, exception));
        }catch (Throwable t) {
            throw new ExecutionException("caught event", channel, getClass()+" error when process caught event ." , t);
        }
    }

    private ExecutorService getExecutorService() {
        ExecutorService cexecutor = executor;
        if (cexecutor == null || cexecutor.isShutdown()) { 
            cexecutor = SHARED_EXECUTOR;
        }
        return cexecutor;
    }
}