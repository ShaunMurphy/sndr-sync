package server;

import fm.icelink.DataBuffer;
import icelink.RemoteConnection;
import icelink.RemoteConnection.Header;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import server.annotations.Route;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import com.sndr.logger.SndrLogger;
import com.sndr.proto.SndrBlockProto;
import common.ClientChannel;
import common.Serializer;

public enum Processor {
    INSTANCE;
    private static final Logger logger = SndrLogger.getLogger();
    private final EnumMap<SndrBlockProto.RequestType, Method> routeTable = new EnumMap<>(SndrBlockProto.RequestType.class);
    //private final Class<GeneratedMessageV3> protobuffMessageClass = GeneratedMessageV3.class;
    private final Class<Message.Builder> protobuffMessageClass = Message.Builder.class;

    /**
     * Registers a class to be used when processing requests.
     * @param classToMap
     */
    public final void register(Class<?> classToMap) {
        for(Method method : classToMap.getMethods()) {
            //Only map methods in this class.
            if(!method.getDeclaringClass().equals(classToMap)) {
                continue;
            }
            
            //Look for the annotation.
            Route route = method.getAnnotation(Route.class);
            if(route == null) {
                continue;
            }

            //Improve reflection performance.
            method.setAccessible(true);
            
            //Get the RequestType.
            SndrBlockProto.RequestType type = route.type();
            this.routeTable.put(type, method);

            //Make sure the method is static.
            if(!Modifier.isStatic(method.getModifiers())) {
                logger.log(Level.WARNING, "The method "+method.getName()+" in "+classToMap.getName()+" must be static.");
            }
            
            if(method.getReturnType() == null) {
                logger.log(Level.WARNING, "The method "+method.getName()+" in "+classToMap.getName()+" does not have a return type."
                        + "The expected return type should be extending "+protobuffMessageClass.getName());
            } else if(!protobuffMessageClass.isAssignableFrom(method.getReturnType())) {
                logger.log(Level.WARNING, "The method "+method.getName()+" in "+classToMap.getName()+"\nreturn type is "+method.getReturnType().getName()
                        + ".\nThe expected return type should be extending "+protobuffMessageClass.getName());
            }
        }
    }

    //The client channel must be blocking!
    //TODO Check this.
    /**
     * This processes the request by passing in the request data to the appropriate method. 
     * Then the response is sent back on the channel.
     * @param channel
     * @return
     * @throws IOException
     */
    @SuppressWarnings("resource")
    public boolean process(ClientChannel channel) throws IOException {
        final long start = System.currentTimeMillis();
        InputStream input = channel.getInputStream();
        //Read the header.
        SndrBlockProto.Header header = SndrBlockProto.Header.parseDelimitedFrom(input);
        if(header == null) {
            logger.log(Level.INFO, "The header was null. The client connection was lost because the client disconnected.");
            return false;
        }
        final SndrBlockProto.RequestType type = header.getType();
        
        if(header.getQuantity() > 1) {
            logger.log(Level.SEVERE, "The quantity was > 1. More than 1 isn't supported, yet.");
        }

        if(!routeTable.containsKey(type)) {
            throw new UnsupportedOperationException("The request type "+type.name()+" "
                    + "does not have a method to call. Maybe the controller was not registered.");
        } else {
            Method method = routeTable.get(type);
            //Deserialize the request.
            GeneratedMessageV3 request = Serializer.INSTANCE.parseDelimitedRequest(type, input);

            //Validate request
            boolean valid = Serializer.INSTANCE.validateAuthentication(request);
            if(!valid) {
                //TODO Error.
                logger.log(Level.WARNING, "Protobuf validation failed, ignoring for now.");
            }
            
            //Invoke the proper message.
            GeneratedMessageV3.Builder<?> response = null;
            try {
                Object object = null;
                int methodCount = method.getParameterTypes().length;
                //TODO Determine a better way to do this.
                if(methodCount == 1) {
                    object = method.invoke(this, request);
                } else if(methodCount == 2) {
                    object = method.invoke(this, request, channel);
                }
                 
                if(object instanceof GeneratedMessageV3.Builder<?>) {
                    response = (GeneratedMessageV3.Builder<?>) object;
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to invoke method.", e);
            }

            if(response == null) {
                logger.log(Level.SEVERE, "Did not send a response to the client!");
                return false;
            }

            //response.writeDelimitedTo(channel.getOutputStream());
            Serializer.INSTANCE.writeDelimitedResponse(type, response, channel.getOutputStream());
            logger.log(Level.FINE, "Completed task in {0} ms.", (System.currentTimeMillis() - start));
            return true;
        }
    }

    public boolean process(RemoteConnection connection, Header header, DataBuffer data) {
        
        final long start = System.currentTimeMillis();
        
        /*
        //Read the header.
        SndrBlockProto.Header header = SndrBlockProto.Header.parseDelimitedFrom(input);
        if(header == null) {
            logger.log(Level.INFO, "The header was null. The client connection was lost because the client disconnected.");
            return false;
        }*/
        final SndrBlockProto.RequestType type = header.type;
        
        /*if(header.getQuantity() > 1) {
            logger.log(Level.SEVERE, "The quantity was > 1. More than 1 isn't supported, yet.");
        }*/

        if(!routeTable.containsKey(type)) {
            throw new UnsupportedOperationException("The request type "+type.name()+" "
                    + "does not have a method to call. Maybe the controller was not registered.");
        } else {
            Method method = routeTable.get(type);
            //Deserialize the request.
            try(ByteArrayInputStream bais = new ByteArrayInputStream(data.getData());) {
                GeneratedMessageV3 request = Serializer.INSTANCE.parseRequest(type, bais);
                //Validate request
                boolean valid = Serializer.INSTANCE.validateAuthentication(request);
                if(!valid) {
                    //TODO Error.
                    logger.log(Level.WARNING, "Protobuf validation failed, ignoring for now.");
                }

                //Invoke the proper message.
                GeneratedMessageV3.Builder<?> response = null;
                try {
                    Object object = method.invoke(this, request);
                    if(object instanceof GeneratedMessageV3.Builder<?>) {
                        response = (GeneratedMessageV3.Builder<?>) object;
                    }
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    logger.log(Level.SEVERE, "Failed to invoke method.", e);
                }

                if (response == null) {
                    logger.log(Level.SEVERE, "Did not send a response to the client!");
                    return false;
                }

                //At this point, the response should be ready to send back.
                //response.writeDelimitedTo(channel.getOutputStream());
                Serializer.INSTANCE.writeDelimitedResponse(type, response, connection);
                logger.log(Level.FINE, "Completed task in {0} ms.", (System.currentTimeMillis() - start));
                return true;
            } catch(IOException e) {
                logger.log(Level.SEVERE, "Failed to process", e);
            }
        }
        return false;
    }
    
    public boolean process(RemoteConnection connection, InputStream input) throws IOException {
        
        final long start = System.currentTimeMillis();
        
        //*
        //Read the header.
        SndrBlockProto.Header header = SndrBlockProto.Header.parseDelimitedFrom(input);
        if(header == null) {
            logger.log(Level.INFO, "The header was null. The client connection was lost because the client disconnected.");
            return false;
        }//*/
        final SndrBlockProto.RequestType type = header.getType();
        
        /*if(header.getQuantity() > 1) {
            logger.log(Level.SEVERE, "The quantity was > 1. More than 1 isn't supported, yet.");
        }*/

        if(!routeTable.containsKey(type)) {
            throw new UnsupportedOperationException("The request type "+type.name()+" "
                    + "does not have a method to call. Maybe the controller was not registered.");
        } else {
            Method method = routeTable.get(type);
            //Deserialize the request.
            GeneratedMessageV3 request = Serializer.INSTANCE.parseDelimitedRequest(type, input);
            // Validate request
            boolean valid = Serializer.INSTANCE.validateAuthentication(request);
            if (!valid) {
                // TODO Error.
                logger.log(Level.WARNING, "Protobuf validation failed, ignoring for now.");
            }

            // Invoke the proper message.
            GeneratedMessageV3.Builder<?> response = null;
            try {
                Object object = method.invoke(this, request);
                if (object instanceof GeneratedMessageV3.Builder<?>) {
                    response = (GeneratedMessageV3.Builder<?>) object;
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                logger.log(Level.SEVERE, "Failed to invoke method.", e);
            }

            if (response == null) {
                // TODO Error?
                logger.log(Level.SEVERE, "Did not send a response to the client!");
                return false;
            }

            Serializer.INSTANCE.writeDelimitedResponse(type, response, connection);
            // At this point, the response should be ready to send back.
            logger.log(Level.FINE, "Completed task in {0} ms.", (System.currentTimeMillis() - start));
            return true;
        }
    }   
}