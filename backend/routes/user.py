from flask import Blueprint, request, jsonify
from db import db_session
from models import User

userRouter = Blueprint('user', __name__)

@userRouter.route('/profile/<int:user_id>', methods=['GET'])
def profile_info(user_id):
    user = db_session.query(User).filter_by(id=user_id).first()
    if user:
        return jsonify(user.getProfileInfo()), 200
    else:
        return jsonify({'error': 'User not found'}), 404

