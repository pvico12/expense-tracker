from marshmallow import Schema, fields, validate

class LoginRequest(Schema):
    username = fields.Str(required=True, validate=validate.Length(min=1))
    password = fields.Str(required=True, validate=validate.Length(min=1))

class RegistrationRequest(Schema):
    username = fields.Str(required=True, validate=validate.Length(min=1))
    password = fields.Str(required=True, validate=validate.Length(min=1))
    firstname = fields.Str(required=True, validate=validate.Length(min=1))
    lastname = fields.Str(required=True, validate=validate.Length(min=1))
    
class TokenRefreshRequest(Schema):
    refresh_token = fields.Str(required=True, validate=validate.Length(min=1))