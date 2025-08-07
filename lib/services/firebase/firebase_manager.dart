import 'package:firebase_core/firebase_core.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_storage/firebase_storage.dart';

class FirebaseManager {
  static FirebaseManager? _instance;
  static FirebaseManager get instance {
    _instance ??= FirebaseManager._();
    return _instance!;
  }
  
  FirebaseManager._();
  
  FirebaseFirestore get firestore => FirebaseFirestore.instance;
  FirebaseAuth get auth => FirebaseAuth.instance;
  FirebaseStorage get storage => FirebaseStorage.instance;
  
  Future<void> initialize() async {
    if (Firebase.apps.isEmpty) {
      await Firebase.initializeApp();
    }
  }
  
  CollectionReference<Map<String, dynamic>> collection(String path) {
    return firestore.collection(path);
  }
  
  Future<DocumentSnapshot<Map<String, dynamic>>> getDocument(
    String collection,
    String documentId,
  ) async {
    return await firestore.collection(collection).doc(documentId).get();
  }
  
  Future<void> setDocument(
    String collection,
    String documentId,
    Map<String, dynamic> data,
  ) async {
    await firestore.collection(collection).doc(documentId).set(data);
  }
  
  Future<void> updateDocument(
    String collection,
    String documentId,
    Map<String, dynamic> data,
  ) async {
    await firestore.collection(collection).doc(documentId).update(data);
  }
  
  Future<void> deleteDocument(
    String collection,
    String documentId,
  ) async {
    await firestore.collection(collection).doc(documentId).delete();
  }
  
  Stream<QuerySnapshot<Map<String, dynamic>>> streamCollection(
    String collection, {
    Query<Map<String, dynamic>>? Function(Query<Map<String, dynamic>>)? queryBuilder,
  }) {
    Query<Map<String, dynamic>> query = firestore.collection(collection);
    if (queryBuilder != null) {
      query = queryBuilder(query);
    }
    return query.snapshots();
  }
  
  Future<String> uploadFile(
    String path,
    dynamic file, {
    Map<String, String>? metadata,
  }) async {
    final ref = storage.ref().child(path);
    final uploadTask = await ref.putData(file);
    return await uploadTask.ref.getDownloadURL();
  }
  
  Future<void> deleteFile(String path) async {
    await storage.ref().child(path).delete();
  }
}