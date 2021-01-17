package cc.nohit.realip;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import cc.nohit.realip.netty.NettyChannelInitializer;

public class RealIP extends JavaPlugin {
	
	public void onLoad(){
		try {
			System.out.println("Injecting NettyHandler...");
			inject();
			System.out.println("Injection successful!");
		} catch (Exception e) {
			System.out.println("Injection netty handler failed!");
			e.printStackTrace();
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private void inject() throws Exception {
		Method serverGetHandle = Bukkit.getServer().getClass().getDeclaredMethod("getServer");
		Object minecraftServer = serverGetHandle.invoke(Bukkit.getServer());
		
		Method serverConnectionMethod = null;
		for(Method method : minecraftServer.getClass().getSuperclass().getDeclaredMethods()) {
			if(!method.getReturnType().getSimpleName().equals("ServerConnection")) {
				continue;
			}
			serverConnectionMethod = method;
			break;
		}
		Object serverConnection = serverConnectionMethod.invoke(minecraftServer);
		List<ChannelFuture> channelFutureList = null;
		for (Field field : serverConnection.getClass().getDeclaredFields()) {
			if (field.getType().getName().contains("List") ) {
				if (((Class<?>)((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0]).getName().contains("ChannelFuture")) {
					field.setAccessible(true);
					channelFutureList = (List<ChannelFuture>) field.get(serverConnection);
				}
			}
		}
		if (channelFutureList == null) {
			throw new Exception("Failed to get channelFutureList.");
		}
		
		for (ChannelFuture channelFuture : channelFutureList) {
			ChannelPipeline channelPipeline = channelFuture.channel().pipeline();
			ChannelHandler serverBootstrapAcceptor = channelPipeline.first();
			System.out.println(serverBootstrapAcceptor.getClass().getName());
			ChannelInitializer<SocketChannel> oldChildHandler = ReflectionUtils.getPrivateField(serverBootstrapAcceptor.getClass(), serverBootstrapAcceptor, ChannelInitializer.class, "childHandler");
			ReflectionUtils.setFinalField(serverBootstrapAcceptor.getClass(), serverBootstrapAcceptor, "childHandler", new NettyChannelInitializer(oldChildHandler));
		}
	}
}
